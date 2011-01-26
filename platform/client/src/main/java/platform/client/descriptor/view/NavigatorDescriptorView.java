package platform.client.descriptor.view;

import platform.base.context.IncrementView;
import platform.base.context.Lookup;
import platform.client.Main;
import platform.client.descriptor.FormDescriptor;
import platform.client.navigator.ClientNavigator;
import platform.client.navigator.ClientNavigatorElement;
import platform.client.navigator.NavigatorTreeNode;
import platform.client.tree.ClientTreeNode;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.List;

public class NavigatorDescriptorView extends JPanel {
    private FormDescriptorView formView;
    private VisualSetupNavigator visualNavigator;

    private Map<String, FormDescriptor> newForms = new HashMap<String, FormDescriptor>();
    private Map<String, ClientNavigatorElement> newElements = new HashMap<String, ClientNavigatorElement>();
    private Map<String, FormDescriptor> changedForms = new HashMap<String, FormDescriptor>();

    private JButton previewBtn;
    private JButton saveBtn;
    private JButton cancelBtn;
    private boolean hasChangedNodes = false;

    private final IncrementView captionUpdater = new IncrementView() {
        public void update(Object updateObject, String updateField) {
            if (updateObject instanceof FormDescriptor) {
                FormDescriptor form = (FormDescriptor) updateObject;
                Enumeration<ClientTreeNode> nodes = visualNavigator.getTree().rootNode.depthFirstEnumeration();
                while (nodes.hasMoreElements()) {
                    ClientTreeNode node = nodes.nextElement();
                    if (node instanceof NavigatorTreeNode) {
                        NavigatorTreeNode navigatorNode = (NavigatorTreeNode) node;
                        if (navigatorNode.navigatorElement.ID == form.ID) {
                            navigatorNode.navigatorElement.setCaption(form.getCaption());
                            updateTree();
                            break;
                        }
                    }
                }
            }
        }
    };

    private void updateTree() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                visualNavigator.getTree().updateUI();
            }
        });
    }

    private final IncrementView updateHandler = new IncrementView() {
        public void update(Object updateObject, String updateField) {
            if (formView.getUpdated()) {
                FormDescriptor currentForm = formView.getForm();
                if (currentForm != null) {
                    changedForms.put(currentForm.getSID(), currentForm);
                    updateTree();
                    setupActionButtons();
                }
            }
        }
    };

    public NavigatorDescriptorView(final ClientNavigator clientNavigator) {

        setLayout(new BorderLayout());

        visualNavigator = new VisualSetupNavigator(this, clientNavigator.remoteNavigator);

        formView = new FormDescriptorView();

        previewBtn = new JButton("Предпросмотр формы");
        previewBtn.setEnabled(false);
        previewBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                PreviewDialog dlg = new PreviewDialog(clientNavigator, formView.getForm());
                dlg.setBounds(SwingUtilities.windowForComponent(NavigatorDescriptorView.this).getBounds());
                dlg.setVisible(true);
            }
        });

        saveBtn = new JButton("Сохранить изменения");
        saveBtn.setEnabled(false);
        saveBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                commitChanges();
            }
        });

        cancelBtn = new JButton("Отменить изменения");
        cancelBtn.setEnabled(false);
        cancelBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                cancelChanges();
            }
        });

        JPanel commandPanel = new JPanel();
        commandPanel.setLayout(new BoxLayout(commandPanel, BoxLayout.X_AXIS));
        commandPanel.add(Box.createRigidArea(new Dimension(5, 5)));
        commandPanel.add(previewBtn);
        commandPanel.add(saveBtn);
        commandPanel.add(Box.createRigidArea(new Dimension(20, 5)));
        commandPanel.add(cancelBtn);
        commandPanel.add(Box.createHorizontalGlue());

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(visualNavigator), formView);
        splitPane.setResizeWeight(0.1);

        add(splitPane, BorderLayout.CENTER);
        add(commandPanel, BorderLayout.SOUTH);
    }

    private void cancelChanges() {
        try {
            visualNavigator.cancelNavigatorChanges();

            while (newForms.size() > 0) {
                FormDescriptor form = newForms.values().iterator().next();
                removeElement(form.getSID());
                newForms.remove(form.getSID());
            }

            formView.setUpdated(false);
            changedForms.clear();

            FormDescriptor currentForm = formView.getForm();
            if (currentForm != null) {
                openForm(currentForm.getSID());
            }
        } catch (IOException e) {
            throw new RuntimeException("Не могу открыть форму.", e);
        }

        hasChangedNodes = false;

        setupActionButtons();
    }

    private void commitChanges() {
        try {
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            DataOutputStream dataStream = new DataOutputStream(outStream);

            //сохраняем элементы
            dataStream.writeInt(newElements.size());
            for (ClientNavigatorElement element : newElements.values()) {
                int bytesWritten = outStream.size();
                element.serialize(dataStream);
                int elementSize = outStream.size() - bytesWritten;
                dataStream.writeInt(elementSize);
            }

            //сохраняем формы
            changedForms.putAll(newForms);
            dataStream.writeInt(changedForms.size());
            for (FormDescriptor form : changedForms.values()) {
                int bytesWritten = outStream.size();
                form.serialize(dataStream);
                int formSize = outStream.size() - bytesWritten;
                dataStream.writeInt(formSize);
            }

            //сохраняем новую структуру навигатора
            Map<String, List<String>> changedElements = getChangedNavigatorElementsChildren();
            dataStream.writeInt(changedElements.size());
            for (Map.Entry<String, List<String>> entry : changedElements.entrySet()) {
                dataStream.writeUTF(entry.getKey());
                dataStream.writeInt(entry.getValue().size());
                for (String childSID : entry.getValue()) {
                    dataStream.writeUTF(childSID);
                }
            }

            visualNavigator.remoteNavigator.saveVisualSetup(outStream.toByteArray());

            formView.setUpdated(false);
            changedForms.clear();
            newForms.clear();
            newElements.clear();
            FormDescriptor currentForm = formView.getForm();
            if (currentForm != null) {
                openForm(currentForm.getSID());
            }
        } catch (IOException e) {
            throw new RuntimeException("Не могу сохранить форму.", e);
        }

        hasChangedNodes = false;

        setupActionButtons();
    }

    private Map<String, List<String>> getChangedNavigatorElementsChildren() {
        HashMap<String, List<String>> result = new HashMap<String, List<String>>();
        Enumeration<ClientTreeNode> nodes = visualNavigator.getTree().rootNode.depthFirstEnumeration();
        while (nodes.hasMoreElements()) {
            ClientTreeNode node = nodes.nextElement();
            if (node instanceof NavigatorTreeNode) {
                NavigatorTreeNode navigatorNode = (NavigatorTreeNode) node;
                if (navigatorNode.nodeStructureChanged) {
                    navigatorNode.nodeStructureChanged = false;
                    List<String> children = new ArrayList<String>();
                    for (int i = 0; i < navigatorNode.getChildCount(); ++i) {
                        ClientTreeNode childNode = (ClientTreeNode) navigatorNode.getChildAt(i);
                        if (childNode instanceof NavigatorTreeNode) {
                            NavigatorTreeNode childNavigatorNode = (NavigatorTreeNode) childNode;
                            children.add(childNavigatorNode.navigatorElement.sID);
                        }
                    }
                    result.put(navigatorNode.navigatorElement.sID, children);
                }
            }
        }

        return result;
    }

    public void openForm(String sID) throws IOException {
        FormDescriptor form = changedForms.get(sID);
        if (form == null) {
            if (newForms.containsKey(sID)) {
                form = newForms.get(sID);
            } else {
                form = FormDescriptor.deserialize(visualNavigator.remoteNavigator.getRichDesignByteArray(sID),
                                                  visualNavigator.remoteNavigator.getFormEntityByteArray(sID));
            }
        }

        formView.setForm(form);

        form.removeDependency(captionUpdater);
        form.addDependency(form, "caption", captionUpdater);
        form.addDependency(form, "updated", updateHandler);

        setupActionButtons();
    }

    public void removeElement(String elementSID) {
        FormDescriptor currentForm = formView.getForm();
        if (currentForm != null && currentForm.getSID().equals(elementSID)) {
            currentForm.getContext().setProperty(Lookup.DELETED_OBJECT_PROPERTY, currentForm);
        }
        changedForms.remove(elementSID);
        newForms.remove(elementSID);
        newElements.remove(elementSID);
    }

    private void setupActionButtons() {
        boolean hasChanges = !changedForms.isEmpty() || !newForms.isEmpty() || !newElements.isEmpty() || hasChangedNodes;

        previewBtn.setEnabled(formView.getForm() != null);
        saveBtn.setEnabled(hasChanges);
        cancelBtn.setEnabled(hasChanges);

        updateUI();
    }

    public boolean isFormChanged(String formSID) {
        return changedForms.containsKey(formSID);
    }

    public FormDescriptor createAndOpenNewForm() {
        FormDescriptor newForm = new FormDescriptor(null);

        newForms.put(newForm.getSID(), newForm);

        try {
            openForm(newForm.getSID());
        } catch (IOException e) {
            throw new RuntimeException("Ошибка при открытии формы.", e);
        }

        return newForm;
    }

    public ClientNavigatorElement createNewNavigatorElement(String caption) {
        ClientNavigatorElement newElement = new ClientNavigatorElement(Main.generateNewID(), caption, caption, false);

        newElements.put(newElement.getSID(), newElement);

        return newElement;
    }

    public void cancelForm(String formSID) {
        FormDescriptor form = changedForms.get(formSID);
        if (form != null) {
            if (newForms.containsKey(formSID)) {
                form = new FormDescriptor(formSID);
                newForms.put(formSID, form);
            }

            changedForms.remove(formSID);
            FormDescriptor currentForm = formView.getForm();
            if (currentForm != null && currentForm.getSID().equals(formSID)) {
                try {
                    openForm(formSID);
                } catch (IOException e) {
                    throw new RuntimeException("Ошибка при отмене формы.", e);
                }
            }
        }
    }

    public void nodeChanged(NavigatorTreeNode node) {
        hasChangedNodes = true;
        setupActionButtons();
    }
}
