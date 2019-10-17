package lsfusion.server.logics.form.interactive.design.auto;

import com.google.common.base.Optional;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import lsfusion.base.BaseUtils;
import lsfusion.interop.base.view.FlexAlignment;
import lsfusion.interop.form.design.Alignment;
import lsfusion.interop.form.design.ContainerFactory;
import lsfusion.interop.form.design.ContainerType;
import lsfusion.interop.form.event.KeyInputEvent;
import lsfusion.interop.form.event.KeyStrokes;
import lsfusion.interop.form.event.MouseInputEvent;
import lsfusion.interop.form.property.PropertyEditType;
import lsfusion.server.base.version.Version;
import lsfusion.server.logics.form.interactive.design.ContainerView;
import lsfusion.server.logics.form.interactive.design.FormContainerSet;
import lsfusion.server.logics.form.interactive.design.FormView;
import lsfusion.server.logics.form.interactive.design.filter.RegularFilterGroupView;
import lsfusion.server.logics.form.interactive.design.filter.RegularFilterView;
import lsfusion.server.logics.form.interactive.design.object.GroupObjectContainerSet;
import lsfusion.server.logics.form.interactive.design.object.GroupObjectView;
import lsfusion.server.logics.form.interactive.design.object.TreeGroupContainerSet;
import lsfusion.server.logics.form.interactive.design.object.TreeGroupView;
import lsfusion.server.logics.form.interactive.design.property.PropertyDrawView;
import lsfusion.server.logics.form.interactive.design.property.PropertyGroupContainerView;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.struct.filter.RegularFilterEntity;
import lsfusion.server.logics.form.struct.filter.RegularFilterGroupEntity;
import lsfusion.server.logics.form.struct.group.Group;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
import lsfusion.server.logics.form.struct.object.TreeGroupEntity;
import lsfusion.server.logics.form.struct.property.PropertyDrawEntity;

import javax.swing.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.synchronizedMap;

public class DefaultFormView extends FormView {
    private PropertyGroupContainerView getPropertyContainer(PropertyDrawEntity property, Version version) {
        return getPropertyGroupContainer(property.getNFToDraw(entity, version));
    }

    private PropertyGroupContainerView getPropertyGroupContainer(GroupObjectEntity groupObject) {
        if(groupObject == null)
            return null;
        if (groupObject.isInTree())
            return get(groupObject.treeGroup);
        return get(groupObject);
    }

    protected transient final Map<PropertyGroupContainerView, ContainerView> boxContainers = synchronizedMap(new HashMap<>());
    public ContainerView getBoxContainer(PropertyGroupContainerView groupObject) { return boxContainers.get(groupObject); }
    public ContainerView getBoxContainer(GroupObjectEntity groupObject) { return getBoxContainer(get(groupObject)); }

    protected transient final Map<PropertyGroupContainerView, ContainerView> gridContainers = synchronizedMap(new HashMap<>());

    protected transient final Map<PropertyGroupContainerView, ContainerView> panelContainers = synchronizedMap(new HashMap<>());
    public ContainerView getPanelContainer(PropertyDrawEntity property, Version version) {return panelContainers.get(getPropertyContainer(property, version)); }

    protected final Map<PropertyGroupContainerView, ContainerView> groupContainers = synchronizedMap(new HashMap<>());
    public ContainerView getPanelPropsContainer(PropertyDrawEntity property, Version version) { return groupContainers.get(getPropertyContainer(property, version)); }

    protected transient final Map<PropertyGroupContainerView, ContainerView> toolbarBoxContainers = synchronizedMap(new HashMap<>());

    protected final Map<PropertyGroupContainerView, ContainerView> toolbarContainers = synchronizedMap(new HashMap<>());
    public ContainerView getToolbarPropsContainer(PropertyDrawEntity property, Version version) { return toolbarContainers.get(getPropertyContainer(property, version)); }

    protected transient final Map<PropertyGroupContainerView, ContainerView> toolbarLeftContainers = synchronizedMap(new HashMap<>());
    protected transient final Map<PropertyGroupContainerView, ContainerView> toolbarRightContainers = synchronizedMap(new HashMap<>());

    protected final Map<PropertyGroupContainerView,ContainerView> filtersContainers = synchronizedMap(new HashMap<>());
    public ContainerView getFilterContainer(GroupObjectEntity groupObject) { return filtersContainers.get(getPropertyGroupContainer(groupObject)); }

    protected transient final Table<Optional<PropertyGroupContainerView>, Group, ContainerView> groupPropertyContainers = HashBasedTable.create();

    public ContainerView objectsContainer;
    public ContainerView toolbarBoxContainer;
    public ContainerView panelContainer;
    public ContainerView groupContainer;
    public ContainerView toolbarContainer;

    private ContainerFactory<ContainerView> containerFactory = () -> new ContainerView(idGenerator.idShift());

    public DefaultFormView() {
    }

    public DefaultFormView(FormEntity formEntity, Version version) {
        super(formEntity, version);

        caption = entity.getCaption();
        canonicalName = entity.getSID();
        creationPath = entity.getCreationPath();
        autoRefresh = entity.autoRefresh;

        FormContainerSet formSet = FormContainerSet.fillContainers(this, containerFactory, new ContainerView.VersionContainerAdder(version));
        
        objectsContainer = formSet.getObjectsContainer();
        addComponentToMapping(objectsContainer, version);
        
        toolbarBoxContainer = formSet.getToolbarBoxContainer();
        addComponentToMapping(toolbarBoxContainer, version);
        
        panelContainer = formSet.getPanelContainer();
        registerComponent(panelContainers, panelContainer, null, version);
        
        groupContainer = formSet.getGroupContainer();
        registerComponent(groupContainers, groupContainer, null, version);
        
        toolbarContainer = formSet.getToolbarContainer();
        registerComponent(toolbarContainers, toolbarContainer, null, version);

        TreeGroupEntity prevTree = null;
        for (GroupObjectView groupObject : getNFGroupObjectsListIt(version)) {
            TreeGroupEntity newTree = groupObject.entity.treeGroup;
            if(prevTree != null && !BaseUtils.nullEquals(prevTree, newTree))
                addTreeGroupView(get(prevTree), version);            
            prevTree = newTree;

            addGroupObjectView(groupObject, version);
        }
        if(prevTree != null)
            addTreeGroupView(get(prevTree), version);

        for (PropertyDrawView propertyDraw : getNFPropertiesListIt(version)) {
            addPropertyDrawView(propertyDraw, version);
        }

        for (RegularFilterGroupView filterGroup : getNFRegularFiltersListIt(version)) {
            addRegularFilterGroupView(filterGroup, version);
        }

        initFormButtons(version);
    }

    public static String getToolbarBoxContainerSID(String goName) {
        return GroupObjectContainerSet.TOOLBARBOX_CONTAINER + "(" + goName + ")";
    }

    public static String getToolbarRightContainerSID(String goName) {
        return GroupObjectContainerSet.TOOLBARRIGHT_CONTAINER + "(" + goName + ")";
    }

    public static String getToolbarLeftContainerSID(String goName) {
        return GroupObjectContainerSet.TOOLBARLEFT_CONTAINER + "(" + goName + ")";
    }

    public static String getFilterGroupsContainerSID(String goName) {
        return GroupObjectContainerSet.FILTERGROUPS_CONTAINER + "(" + goName + ")";
    }

    public static String getGridBoxContainerSID(String goName) {
        return GroupObjectContainerSet.GRIDBOX_CONTAINER + "(" + goName + ")";
    }

    public static String getBoxContainerSID(String goName) {
        return GroupObjectContainerSet.BOX_CONTAINER + "(" + goName + ")";
    }

    public static String getPanelContainerSID(String goName) {
        return GroupObjectContainerSet.PANEL_CONTAINER + "(" + goName + ")";
    }

    public static String getToolbarContainerSID(String goName) {
        return GroupObjectContainerSet.TOOLBAR_CONTAINER + "(" + goName + ")";
    }

    public static String getGOGroupContainerSID(String goName) {
        return GroupObjectContainerSet.GROUP_CONTAINER + "(" + goName + ")";
    }

    public static String getGroupContainerSID(String pgName) {
        return FormContainerSet.GROUP_CONTAINER + "(" + pgName + ")";
    }

    public static String getObjectsContainerSID() {
        return FormContainerSet.OBJECTS_CONTAINER;
    }

    public static String getToolbarBoxContainerSID() {
        return FormContainerSet.TOOLBARBOX_CONTAINER;
    }

    public static String getToolbarLeftContainerSID() {
        return FormContainerSet.TOOLBARLEFT_CONTAINER;
    }

    public static String getToolbarRightContainerSID() {
        return FormContainerSet.TOOLBARRIGHT_CONTAINER;
    }

    public static String getPanelContainerSID() {
        return FormContainerSet.PANEL_CONTAINER;
    }

    public static String getToolbarContainerSID() {
        return FormContainerSet.TOOLBAR_CONTAINER;
    }

    private void initFormButtons(Version version) {
//        PropertyDrawView printFunction = get(entity.printActionPropertyDraw);
//        setupFormButton(printFunction, KeyStrokes.getPrintKeyStroke(), "print.png");

//        PropertyDrawView xlsFunction = get(entity.xlsActionPropertyDraw);
//        setupFormButton(xlsFunction, KeyStrokes.getXlsKeyStroke(), "xls.png");

        PropertyDrawView editFunction = get(entity.editActionPropertyDraw);
        setupFormButton(editFunction, KeyStrokes.getEditKeyStroke(), "editReport.png");

        PropertyDrawView dropFunction = get(entity.dropActionPropertyDraw);
        setupFormButton(dropFunction, KeyStrokes.getNullKeyStroke());

        PropertyDrawView refreshFunction = get(entity.refreshActionPropertyDraw);
        setupFormButton(refreshFunction, KeyStrokes.getRefreshKeyStroke(), "refresh.png");
        refreshFunction.drawAsync = true;

        PropertyDrawView applyFunction = get(entity.applyActionPropertyDraw);
        setupFormButton(applyFunction, KeyStrokes.getApplyKeyStroke());

        PropertyDrawView cancelFunction = get(entity.cancelActionPropertyDraw);
        setupFormButton(cancelFunction, KeyStrokes.getCancelKeyStroke());

        PropertyDrawView okFunction = get(entity.okActionPropertyDraw);
        setupFormButton(okFunction, KeyStrokes.getOkKeyStroke(), new MouseInputEvent(MouseInputEvent.DBLCLK), 1000, true);

        PropertyDrawView closeFunction = get(entity.closeActionPropertyDraw);
        setupFormButton(closeFunction, KeyStrokes.getCloseKeyStroke());

        PropertyDrawView logMessage = get(entity.logMessagePropertyDraw);

        ContainerView toolbarLeftContainer = createContainer(null, null, DefaultFormView.getToolbarLeftContainerSID(), version);
        toolbarLeftContainer.setType(ContainerType.CONTAINERH);
        toolbarLeftContainer.childrenAlignment = Alignment.START;
        toolbarLeftContainer.setFlex(0);

        ContainerView toolbarRightContainer = createContainer(null, null, DefaultFormView.getToolbarRightContainerSID(), version);
        toolbarRightContainer.setType(ContainerType.CONTAINERH);
        toolbarRightContainer.childrenAlignment = Alignment.END;
        toolbarRightContainer.setFlex(1);

        toolbarLeftContainer.add(editFunction, version);
        toolbarLeftContainer.add(logMessage, version); // otherwise it will go to OBJECTS container which has types COLUMNS and this type doesnt respect SHOWIF

        toolbarRightContainer.add(toolbarContainer, version);
        toolbarRightContainer.add(refreshFunction, version);
        toolbarRightContainer.add(dropFunction, version);
        toolbarRightContainer.add(applyFunction, version);
        toolbarRightContainer.add(cancelFunction, version);
        toolbarRightContainer.add(okFunction, version);
        toolbarRightContainer.add(closeFunction, version);

        toolbarBoxContainer.add(toolbarLeftContainer, version);
        toolbarBoxContainer.add(toolbarRightContainer, version);
    }
    
    private void setupFormButton(PropertyDrawView action, KeyStroke editKey, MouseInputEvent mouseInputEvent, int mousePriority, boolean mouseOnlyDialog) {
        setupFormButton(action, editKey, null, mouseInputEvent, mousePriority, mouseOnlyDialog);
    }
    private void setupFormButton(PropertyDrawView action, KeyStroke editKey) {
        setupFormButton(action, editKey, (String)null);        
    }
    private void setupFormButton(PropertyDrawView action, KeyStroke editKey, String imagePath) {
        setupFormButton(action, editKey, imagePath, null, 0, false);
    }
    private void setupFormButton(PropertyDrawView action, KeyStroke editKey, String imagePath, MouseInputEvent mouseEvent, int mousePriority, boolean mouseOnlyDialog) {
        action.changeKey = editKey != null ? new KeyInputEvent(editKey) : null;
        action.showChangeKey = false;
        action.changeMouse = mouseEvent;
        action.changeMousePriority = mousePriority;
        action.changeMouseOnlyDialog = mouseOnlyDialog;
        action.focusable = false;
        action.entity.setEditType(PropertyEditType.EDITABLE);
        action.setAlignment(FlexAlignment.STRETCH);

        if (imagePath != null) {
            action.design.setImagePath(imagePath);
        }
    }

    private void addGroupObjectView(GroupObjectView groupObject, Version version) {
        addGroupObjectView(groupObject, null, false, version);
    }
    private void addGroupObjectView(GroupObjectView groupObject, GroupObjectEntity neighbourGroupObject, boolean isRightNeigbour, Version version) {
        if(!groupObject.entity.isInTree()) {
            GroupObjectContainerSet groupSet = GroupObjectContainerSet.create(groupObject, containerFactory, new ContainerView.VersionContainerAdder(version));

            addPropertyGroupContainerView(groupSet.getBoxContainer(), neighbourGroupObject, isRightNeigbour, version);

            registerComponent(boxContainers, groupSet.getBoxContainer(), groupObject, version);
            registerComponent(gridContainers, groupSet.getGridBoxContainer(), groupObject, version);
            registerComponent(panelContainers, groupSet.getPanelContainer(), groupObject, version);
            registerComponent(groupContainers, groupSet.getGroupContainer(), groupObject, version);
            registerComponent(toolbarBoxContainers, groupSet.getToolbarBoxContainer(), groupObject, version);
            registerComponent(toolbarLeftContainers, groupSet.getToolbarLeftContainer(), groupObject, version);
            registerComponent(toolbarRightContainers, groupSet.getToolbarRightContainer(), groupObject, version);
            registerComponent(filtersContainers, groupSet.getFiltersContainer(), groupObject, version);
            registerComponent(toolbarContainers, groupSet.getToolbarContainer(), groupObject, version);

            addClassChoosers(groupSet.getGridBoxContainer(), groupObject, version);

            if (groupObject.entity.isForcedPanel()) { // если groupObject идет в панель, то grid'а быть не может, и если box не выставить не 0, он не будет брать весь размер
                groupSet.getBoxContainer().setFlex(0);
            }
        }
    }

    private void addPropertyGroupContainerView(ContainerView boxContainer, GroupObjectEntity neighbour, boolean isRightNeigbour, Version version) {
        PropertyGroupContainerView neighbourView = neighbour != null ? (neighbour.isInTree() ? get(neighbour.treeGroup) : get(neighbour)) : null;
        if(neighbourView != null) {
            ContainerView neighbourBox = getBoxContainer(neighbourView);
            if(isRightNeigbour)
                objectsContainer.addAfter(boxContainer, neighbourBox, version);
            else
                objectsContainer.addBefore(boxContainer, neighbourBox, version);
        } else {
            objectsContainer.add(boxContainer, version);
        }
    }

    private void addClassChoosers(ContainerView gridBox, GroupObjectView groupObject, Version version) {
        if (groupObject.size() == 1) {
            gridBox.addFirst(groupObject.get(0).classChooser, version);
        } else if (groupObject.size() > 1) {
            List<ContainerView> containers = new ArrayList<>();
            for (int i = 0; i < groupObject.size() - 1; i++) {
                ContainerView container = createContainer(version);
                container.setType(ContainerType.HORIZONTAL_SPLIT_PANE);
                container.add(groupObject.get(i).classChooser, version);
                containers.add(container);
            }
            containers.get(containers.size() - 1).add(groupObject.get(groupObject.size() - 1).classChooser, version);
            for (int i = containers.size() - 1; i > 0; i--) {
                containers.get(i - 1).add(containers.get(i), version);
            }
            gridBox.addFirst(containers.get(0), version);
        }
    }

    // сейчас во многом повторяет addGroupObjectView, потом надо рефакторить
    private void addTreeGroupView(TreeGroupView treeGroup, Version version) {
        addTreeGroupView(treeGroup, null, false, version);
    }
    private void addTreeGroupView(TreeGroupView treeGroup, GroupObjectEntity neighbour, boolean isRightNeigbour, Version version) {
        TreeGroupContainerSet treeSet = TreeGroupContainerSet.create(treeGroup, containerFactory, new ContainerView.VersionContainerAdder(version));
        
        addPropertyGroupContainerView(treeSet.getBoxContainer(), neighbour, isRightNeigbour, version);

        registerComponent(boxContainers, treeSet.getBoxContainer(), treeGroup, version);
        registerComponent(gridContainers, treeSet.getGridContainer(), treeGroup, version);
        registerComponent(panelContainers, treeSet.getPanelContainer(), treeGroup, version);
        registerComponent(groupContainers, treeSet.getGroupContainer(), treeGroup, version);
        registerComponent(toolbarBoxContainers, treeSet.getToolbarBoxContainer(), treeGroup, version);
        registerComponent(toolbarLeftContainers, treeSet.getToolbarLeftContainer(), treeGroup, version);
        registerComponent(toolbarRightContainers, treeSet.getToolbarRightContainer(), treeGroup, version);
        registerComponent(filtersContainers, treeSet.getFiltersContainer(), treeGroup, version);
        registerComponent(toolbarContainers, treeSet.getToolbarContainer(), treeGroup, version);

//        for (GroupObjectEntity group : treeGroup.entity.getGroups()) {
//            ContainerView groupContainer = boxContainers.get(mgroupObjects.get(group));
//            treeSet.getBoxContainer().add(groupContainer, version);
//        }
    }

    private void registerComponent(Map<PropertyGroupContainerView, ContainerView> containers, ContainerView container, PropertyGroupContainerView group, Version version) {
        containers.put(group, container);
        addComponentToMapping(container, version);
    }

    // добавление в панель по сути, так как добавление в grid происходит уже на живой форме
    private void addPropertyDrawView(PropertyDrawView propertyDraw, Version version) {
        PropertyDrawEntity drawEntity = propertyDraw.entity;
        drawEntity.proceedDefaultDesign(propertyDraw, this);

        ContainerView propertyContainer;
        if (propertyDraw.entity.isToolbar(entity)) {
            propertyContainer = getToolbarPropsContainer(drawEntity, version);
            propertyDraw.setAlignment(FlexAlignment.CENTER);
        } else {
            // иерархическая структура контейнеров групп: каждый контейнер группы - это CONTAINERH,
            // в который сначала добавляется COLUMNS для свойств этой группы, а затем - контейнеры подгрупп
            propertyContainer = getPropGroupContainer(drawEntity, propertyDraw.entity.getNFGroup(version), version);
        }

        propertyContainer.add(propertyDraw, version);
    }

    @Override
    public GroupObjectView addGroupObject(GroupObjectEntity groupObject, GroupObjectEntity neighbour, Boolean isRightNeighbour, Version version) {
        GroupObjectView view = super.addGroupObject(groupObject, neighbour, isRightNeighbour, version);
        addGroupObjectView(view, neighbour, isRightNeighbour != null && isRightNeighbour, version);
        return view;
    }

    @Override
    public TreeGroupView addTreeGroup(TreeGroupEntity treeGroup, GroupObjectEntity neighbour, boolean isRightNeighbour, Version version) {
        TreeGroupView view = super.addTreeGroup(treeGroup, neighbour, isRightNeighbour, version);
        addTreeGroupView(view, neighbour, isRightNeighbour, version);
        return view;
    }

    @Override
    public PropertyDrawView addPropertyDraw(PropertyDrawEntity propertyDraw, Version version) {
        PropertyDrawView view = super.addPropertyDraw(propertyDraw, version);
        addPropertyDrawView(view, version);
        return view;
    }

    @Override
    public RegularFilterGroupView addRegularFilterGroup(RegularFilterGroupEntity filterGroup, Version version) {
        RegularFilterGroupView view = super.addRegularFilterGroup(filterGroup, version);
        addRegularFilterGroupView(view, version);
        return view;
    }

    private void addRegularFilterGroupView(RegularFilterGroupView filterGroup, Version version) {
        ContainerView filterContainer = getFilterContainer(filterGroup, version);
        if (filterContainer != null) {
            filterContainer.add(filterGroup, version);
        }
    }

    private ContainerView getFilterContainer(RegularFilterGroupView filterGroup, Version version) {
        GroupObjectEntity groupObject = filterGroup.entity.getNFToDraw(entity, version);
        return getFilterContainer(groupObject);
    }

    @Override
    public RegularFilterView addRegularFilter(RegularFilterGroupEntity filterGroup, RegularFilterEntity filter, Version version) {
        RegularFilterGroupView filterGroupView = get(filterGroup);
        boolean moveGroupToNewContainer = false;
        if (filterGroupView.getNFContainer(version) == getFilterContainer(filterGroupView, version)) {
            //если группа осталась в дефолтном контейнере, то перемещаем её в новое дефолтное место
            moveGroupToNewContainer = true;
        }
        
        RegularFilterView filterView = super.addRegularFilter(filterGroup, filter, version);
        if (moveGroupToNewContainer) {
            addRegularFilterGroupView(filterGroupView, version);
        }
        return filterView;
    }

    //    private void addPropertyDrawToLayout(GroupObjectView groupObject, PropertyDrawView propertyDraw) {
//        Group propertyParentGroup = propertyDraw.entity.propertyObject.property.getParent();
//
//        Pair<ContainerView, ContainerView> groupContainers = getPropGroupContainer(groupObject, propertyParentGroup);
//        groupContainers.second.add(propertyDraw);
//    }
//
//    //возвращает контейнер группы и контейнер свойств этой группы
//    private Pair<ContainerView, ContainerView> getPropGroupContainer(GroupObjectView groupObject, Group currentGroup) {
//        if (currentGroup == null) {
//            return new Pair<ContainerView, ContainerView>(panelContainers.get(groupObject), groupContainers.get(groupObject));
//        }
//
//        if (!currentGroup.createContainer) {
//            return getPropGroupContainer(groupObject, currentGroup.getParent());
//        }
//
//        //ищем в созданных
//        ContainerView currentGroupContainer = groupPropertyContainers.get(Optional.fromNullable(groupObject), currentGroup);
//        ContainerView currentGroupPropsContainer = groupPropertyPropsContainer.get(Optional.fromNullable(groupObject), currentGroup);
//        if (currentGroupContainer == null) {
//            String currentGroupContainerSID = getPropertyGroupContainerSID(groupObject, currentGroup);
//            String currentGroupPropsContainerSID = currentGroupContainerSID + ".props";
//
//            //ищем по имени
//            currentGroupContainer = getContainerBySID(currentGroupContainerSID);
//            if (currentGroupContainer == null) {
//                //не нашли - создаём
//                currentGroupContainer = createContainer(currentGroup.caption, null, currentGroupContainerSID);
//                currentGroupContainer.setType(ContainerType.CONTAINERV);
//
//                currentGroupPropsContainer = createGroupPropsContainer(currentGroupContainer, currentGroupPropsContainerSID);
//
//                groupPropertyPropsContainer.put(Optional.fromNullable(groupObject), currentGroup, currentGroupPropsContainer);
//
//                Pair<ContainerView, ContainerView> parentGroupContainers = getPropGroupContainer(groupObject, currentGroup.getParent());
//                parentGroupContainers.first.add(currentGroupContainer);
//            } else {
//                //нашли контейнер группы по имени
//                currentGroupPropsContainer = getContainerBySID(currentGroupPropsContainerSID);
//                if (currentGroupPropsContainer == null) {
//                    //...но не нашли контейнер свойств по имени
//                    currentGroupPropsContainer = createGroupPropsContainer(currentGroupContainer, currentGroupPropsContainerSID);
//                }
//            }
//
//            groupPropertyContainers.put(Optional.fromNullable(groupObject), currentGroup, currentGroupContainer);
//        }
//
//        return new Pair<ContainerView, ContainerView>(currentGroupContainer, currentGroupPropsContainer);
//    }
//
//    private ContainerView createGroupPropsContainer(ContainerView groupContainer, String currentGroupPropsContainerSID) {
//        ContainerView groupPropsContainer = createContainer(null, null, currentGroupPropsContainerSID);
//        groupPropsContainer.setType(ContainerType.COLUMNS);
//        groupPropsContainer.columns = 6;
//        groupContainer.add(groupPropsContainer);
//        return groupPropsContainer;
//    }

    //возвращает контейнер группы и контейнер свойств этой группы
    private ContainerView getPropGroupContainer(PropertyDrawEntity propertyDraw, Group currentGroup, Version version) {
        if (currentGroup == null) {
            return getPanelPropsContainer(propertyDraw, version);
        }

        if (!currentGroup.createContainer()) {
            return getPropGroupContainer(propertyDraw, currentGroup.getNFParent(version), version);
        }

        ContainerView propGroupContainer;
        PropertyGroupContainerView propertyContainer = getPropertyContainer(propertyDraw, version);
        synchronized (groupPropertyContainers) {
            propGroupContainer = groupPropertyContainers.get(Optional.fromNullable(propertyContainer), currentGroup);
            if(propGroupContainer != null)
                return propGroupContainer;

            // first we'll create containers for upper groups to get right component order
            getPropGroupContainer(propertyDraw, currentGroup.getNFParent(version), version);

            propGroupContainer = createContainer(currentGroup.caption, null, getPropGroupContainerSID(currentGroup, propertyContainer), Version.GLOBAL);
            propGroupContainer.setType(ContainerType.COLUMNS);
            propGroupContainer.columns = 4;

            getPanelContainer(propertyDraw, version).add(propGroupContainer, Version.GLOBAL);

            groupPropertyContainers.put(Optional.fromNullable(propertyContainer), currentGroup, propGroupContainer);
        }

        return propGroupContainer;
    }

    private String getPropGroupContainerSID(Group currentGroup, PropertyGroupContainerView propertyContainer) {
        String propertyGroupName = currentGroup.getCanonicalName();
        String currentGroupContainerSID;
        if (propertyContainer == null)
            currentGroupContainerSID = DefaultFormView.getGroupContainerSID(propertyGroupName);
        else
            currentGroupContainerSID = GroupObjectContainerSet.GROUP_CONTAINER + "(" + propertyGroupName + "," + propertyContainer.getPropertyGroupContainerSID() + ")";
        return currentGroupContainerSID;
    }
}
