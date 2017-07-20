package lsfusion.client.form;

import lsfusion.client.ClientResourceBundle;

import javax.swing.*;
import java.awt.*;

public interface PropertyRenderer {
    Color SELECTED_ROW_BORDER_COLOR = new Color(175, 175, 255);
    Color SELECTED_ROW_BACKGROUND = new Color(249, 249, 255);
    //Color SELECTED_CELL_BACKGROUND = new Color(237, 238, 244);
    //Color FOCUSED_CELL_BORDER_COLOR = new Color(98, 98, 255);
    //Color FOCUSED_CELL_BACKGROUND = new Color(232, 232, 255);
    //Border SELECTED_ROW_BORDER = BorderFactory.createMatteBorder(1, 0, 1, 0, SELECTED_ROW_BORDER_COLOR);
    //Border FOCUSED_CELL_BORDER = BorderFactory.createMatteBorder(1, 1, 1, 1, FOCUSED_CELL_BORDER_COLOR);

    String EMPTY_STRING = ClientResourceBundle.getString("form.renderer.not.defined");
    String REQUIRED_STRING = ClientResourceBundle.getString("form.renderer.required");

    Color NORMAL_FOREGROUND = UIManager.getColor("TextField.foreground");
    Color INACTIVE_FOREGROUND = UIManager.getColor("TextField.inactiveForeground");
    
    Color REQUIRED_FOREGROUND = new Color(136, 9, 0);

    JComponent getComponent();

    void setValue(Object value, boolean isSelected, boolean hasFocus);

    void paintAsSelected();
}

