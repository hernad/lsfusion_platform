package lsfusion.server.logics.scripted.proxy;

import lsfusion.interop.Compare;
import lsfusion.server.classes.DateClass;
import lsfusion.server.classes.IntegralClass;
import lsfusion.server.data.type.Type;
import lsfusion.server.form.view.PropertyDrawView;
import lsfusion.server.logics.PropertyUtils;
import lsfusion.server.logics.i18n.LocalizedString;

import javax.swing.*;
import java.awt.*;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;

public class PropertyDrawViewProxy extends ComponentViewProxy<PropertyDrawView> {

    public PropertyDrawViewProxy(PropertyDrawView target) {
        super(target);
    }

    public void setAutoSize(boolean autoSize) {
        target.autoSize = autoSize;
    }

    public void setPanelCaptionAfter(boolean panelCaptionAfter) {
        target.panelCaptionAfter = panelCaptionAfter;
    }

    public void setEditOnSingleClick(boolean editOnSingleClick) {
        target.editOnSingleClick = editOnSingleClick;
    }

    public void setHide(boolean hide) {
        target.hide = hide;
    }

    public void setRegexp(String regexp) {
        target.regexp = regexp;
    }

    public void setRegexpMessage(String regexpMessage) {
        target.regexpMessage = regexpMessage;
    }

    public void setPattern(String pattern) {
        Type type = target.getType();
        if(type instanceof IntegralClass) {
            target.format = new DecimalFormat(pattern);
        } else if(type instanceof DateClass) {
            target.format = new SimpleDateFormat(pattern);
        }
        target.pattern = pattern;
    }

    public void setMaxValue(long maxValue) {
        target.maxValue = maxValue;
    }

    public void setEchoSymbols(boolean echoSymbols) {
        target.echoSymbols = echoSymbols;
    }

    public void setNoSort(boolean noSort) {
        target.noSort = noSort;
    }

    public void setDefaultCompare(String defaultCompare) {
        target.defaultCompare = PropertyUtils.stringToCompare(defaultCompare);
    }

    public void setMinimumCharWidth(int minimumCharWidth) {
        target.setMinimumCharWidth(minimumCharWidth);
    }

    public void setMaximumCharWidth(int maximumCharWidth) {
        target.setMaximumCharWidth(maximumCharWidth);
    }

    public void setPreferredCharWidth(int preferredCharWidth) {
        target.setPreferredCharWidth(preferredCharWidth);
    }

    public void setMinimumValueSize(Dimension minimumValueSize) {
        target.setMinimumValueSize(minimumValueSize);
    }
    public void setMaximumValueSize(Dimension maximumValueSize) {
        target.setMaximumValueSize(maximumValueSize);
    }
    public void setPreferredValueSize(Dimension preferredValueSize) {
        target.setPreferredValueSize(preferredValueSize);
    }

    public void setMinimumValueHeight(int minHeight) {
        if (target.minimumValueSize == null) {
            target.minimumValueSize = new Dimension(-1, minHeight);
        } else {
            target.minimumValueSize.height = minHeight;
        }
    }

    public void setMinimumValueWidth(int minWidth) {
        if (target.minimumValueSize == null) {
            target.minimumValueSize = new Dimension(minWidth, -1);
        } else {
            target.minimumValueSize.width = minWidth;
        }
    }

    public void setMaximumValueHeight(int maxHeight) {
        if (target.maximumValueSize == null) {
            target.maximumValueSize = new Dimension(-1, maxHeight);
        } else {
            target.maximumValueSize.height = maxHeight;
        }
    }

    public void setMaximumValueWidth(int maxWidth) {
        if (target.maximumValueSize == null) {
            target.maximumValueSize = new Dimension(maxWidth, -1);
        } else {
            target.maximumValueSize.width = maxWidth;
        }
    }

    public void setPreferredValueHeight(int maxHeight) {
        if (target.preferredValueSize == null) {
            target.preferredValueSize = new Dimension(-1, maxHeight);
        } else {
            target.preferredValueSize.height = maxHeight;
        }
    }

    public void setPreferredValueWidth(int maxWidth) {
        if (target.preferredValueSize == null) {
            target.preferredValueSize = new Dimension(maxWidth, -1);
        } else {
            target.preferredValueSize.width = maxWidth;
        }
    }

    public void setFixedValueSize(Dimension size) {
        setMinimumValueSize(size);
        setMaximumValueSize(size);
        setPreferredValueSize(size);
    }

    public void setFixedValueHeight(int height) {
        setMinimumValueHeight(height);
        setMaximumValueHeight(height);
        setPreferredValueHeight(height);
    }

    public void setFixedValueWidth(int width) {
        setMinimumValueWidth(width);
        setMaximumValueWidth(width);
        setPreferredValueWidth(width);
    }

    public void setEditKey(KeyStroke editKey) {
        target.editKey = editKey;
    }

    public void setShowEditKey(boolean showEditKey) {
        target.showEditKey = showEditKey;
    }

    public void setFocusable(Boolean focusable) {
        target.focusable = focusable;
    }

    public void setPanelCaptionAbove(boolean panelCaptionAbove) {
        target.panelCaptionAbove = panelCaptionAbove;
    }

    public void setCaption(String caption) {
        target.caption = LocalizedString.create(caption);
    }

    public void setClearText(boolean clearText) {
        target.clearText = clearText;
    }

    public void setAskConfirm(boolean askConfirm) {
        target.entity.askConfirm = askConfirm;
    }

    public void setAskConfirmMessage(String askConfirmMessage) {
        target.entity.askConfirmMessage = askConfirmMessage;
    }
    
    public void setToolTip(String toolTip) {
        target.toolTip = toolTip;
    }
    
    public void setNotNull(boolean notNull) {
        target.notNull = notNull;
    }
}