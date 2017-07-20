package lsfusion.gwt.form.shared.view.grid.renderer;

import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Style;
import com.google.gwt.i18n.client.NumberFormat;
import lsfusion.gwt.base.client.EscapeUtils;
import lsfusion.gwt.form.shared.view.GPropertyDraw;

public class NumberGridCellRenderer extends TextBasedGridCellRenderer<Number> {
    private NumberFormat defaultFormat;
    protected NumberFormat format;

    public NumberGridCellRenderer(GPropertyDraw property) {
        this(property, NumberFormat.getDecimalFormat());
    }

    public NumberGridCellRenderer(GPropertyDraw property, NumberFormat format) {
        super(property, Style.TextAlign.RIGHT);
        this.format = format;
        this.defaultFormat = format;
    }

    public void setFormat(String pattern) {
        this.format = pattern != null ? NumberFormat.getFormat(pattern) : defaultFormat;
    }

    @Override
    protected String renderToString(Number value) {
        return format.format(value);
    }

    @Override
    protected void setInnerText(DivElement div, String innerText) {
        if (innerText == null) {
            if (property.isEditableNotNull()) {
                div.setInnerText(REQUIRED_VALUE);
                div.addClassName("requiredValueString");
            } else {
                div.setInnerText(EscapeUtils.UNICODE_NBSP);
                div.removeClassName("requiredValueString");
            }
        } else {
            div.setInnerText(innerText);
            div.removeClassName("requiredValueString");
        }
    }
}