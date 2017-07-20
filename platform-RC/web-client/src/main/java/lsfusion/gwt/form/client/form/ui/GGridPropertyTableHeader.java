package lsfusion.gwt.form.client.form.ui;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.*;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Event;
import lsfusion.gwt.base.client.EscapeUtils;
import lsfusion.gwt.base.client.GwtClientUtils;
import lsfusion.gwt.cellview.client.Column;
import lsfusion.gwt.cellview.client.Header;
import lsfusion.gwt.form.shared.view.GPropertyDraw;

import static com.google.gwt.dom.client.BrowserEvents.*;
import static com.google.gwt.dom.client.Style.Cursor;
import static com.google.gwt.user.client.Event.NativePreviewEvent;
import static com.google.gwt.user.client.Event.NativePreviewHandler;
import static lsfusion.gwt.base.client.GwtClientUtils.stopPropagation;
import static lsfusion.gwt.base.shared.GwtSharedUtils.nullEquals;

public class GGridPropertyTableHeader extends Header<String> {
    private static final int ANCHOR_WIDTH = 10;
    private static final int MAX_HEADER_HEIGHT = 30; // должна быть равна .dataGridHeaderCell { height: ...; }

    private final GGridPropertyTable table;

    private ColumnResizeHelper resizeHelper = null;

    private String renderedCaption;
    private Boolean renderedSortDir;

    private Element renderedCaptionElement;

    private String caption;
    private String toolTip;
    
    private boolean notNull;
    private boolean hasChangeAction;

    private int headerHeight;

    public GGridPropertyTableHeader(GGridPropertyTable table, int headerHeight) {
        this(table, null, null, headerHeight);
    }

    public GGridPropertyTableHeader(GGridPropertyTable table, String caption) {
        this(table, caption, null);
    }

    public GGridPropertyTableHeader(GGridPropertyTable table, String caption, String toolTip) {
        this(table, caption, toolTip, 0);
    }

    public GGridPropertyTableHeader(GGridPropertyTable table, String caption, String toolTip, int headerHeight) {
        super(DBLCLICK, MOUSEDOWN, MOUSEMOVE, MOUSEOVER, MOUSEOUT);

        this.caption = caption;
        this.table = table;
        this.toolTip = toolTip;
        this.headerHeight = headerHeight;
    }

    public void setCaption(String caption, boolean notNull, boolean hasChangeAction) {
        this.caption = caption;
        this.notNull = notNull;
        this.hasChangeAction = hasChangeAction;
    }

    public void setToolTip(String toolTip) {
        this.toolTip = toolTip;
    }

    public void setHeaderHeight(int headerHeight) {
        this.headerHeight = headerHeight;
    }

    @Override
    public void onBrowserEvent(Element target, NativeEvent event) {
        String eventType = event.getType();
        if (DBLCLICK.equals(eventType)) {
            stopPropagation(event);
            table.headerClicked(this, event.getCtrlKey());
        } else if (MOUSEMOVE.equals(eventType) || MOUSEDOWN.equals(eventType)) {
            if (resizeHelper == null) {
                int mouseX = event.getClientX();

                int anchorRight = target.getAbsoluteRight() - ANCHOR_WIDTH;
                int anchorLeft = target.getAbsoluteLeft() + ANCHOR_WIDTH;

                int headerIndex = table.getHeaderIndex(this);
                if ((mouseX > anchorRight && headerIndex != table.getColumnCount() - 1) || (mouseX < anchorLeft && headerIndex > 0)) {
                    target.getStyle().setCursor(Cursor.COL_RESIZE);
                    if (eventType.equals(MOUSEDOWN)) {
                        int leftColumnIndex = mouseX > anchorRight ? headerIndex : headerIndex - 1;
                        Column leftColumn = table.getColumn(leftColumnIndex);
                        TableCellElement leftHeaderCell = ((TableRowElement) target.getParentElement()).getCells().getItem(leftColumnIndex);

                        int rightColumnsCount = table.getColumnCount() - leftColumnIndex - 1;
                        Column[] rightColumns = new Column[rightColumnsCount];
                        double[] rightScaleWidths = new double[rightColumnsCount];
                        for (int i = 1; i <= rightColumnsCount; i++) {
                            Column column = table.getColumn(leftColumnIndex + i);
                            rightColumns[i - 1] = column;
                            rightScaleWidths[i - 1] = getColumnWidth(column);
                        }

                        resizeHelper = new ColumnResizeHelper(leftColumn, rightColumns, rightScaleWidths, leftHeaderCell.getAbsoluteRight(),
                                getColumnWidth(leftColumn), leftHeaderCell.getOffsetWidth());
                        stopPropagation(event);
                    }
                } else {
                    target.getStyle().setCursor(Cursor.DEFAULT);
                }
            }
        } else if (MOUSEOVER.equals(eventType)) {
            TooltipManager.get().showTooltip(event.getClientX(), event.getClientY(), new TooltipManager.TooltipHelper() {
                @Override
                public String getTooltip() {
                    return toolTip;
                }

                @Override
                public boolean stillShowTooltip() {
                    return table.isAttached() && table.isVisible();
                }
            });
        } else if (MOUSEOUT.equals(eventType)) {
            TooltipManager.get().hideTooltip();
        }
        if (MOUSEMOVE.equals(eventType)) {
            TooltipManager.get().updateMousePosition(event.getClientX(), event.getClientY());
        }
        if (MOUSEDOWN.equals(eventType)) {
            TooltipManager.get().hideTooltip();
        }
    }

    private double getColumnWidth(Column column) {
        String width = table.getColumnWidth(column);
        return Double.parseDouble(width.substring(0, width.indexOf("px")));
    }

    @Override
    public void renderDom(TableCellElement th) {
        th.addClassName("positionRelative");
        
        Boolean sortDir = table.getSortDirection(this);
        String escapedCaption = getEscapedCaption();

        int maxHeight = headerHeight > 0 ? headerHeight : MAX_HEADER_HEIGHT;
        DivElement div = Document.get().createDivElement();
        div.getStyle().setProperty("maxHeight", maxHeight, Style.Unit.PX);
        div.getStyle().setOverflow(Style.Overflow.HIDDEN);
        div.getStyle().setTextAlign(Style.TextAlign.CENTER);
        div.getStyle().setWhiteSpace(Style.WhiteSpace.NOWRAP);

        if (sortDir != null) {
            ImageElement img = Document.get().createImageElement();
            img.getStyle().setHeight(15, Style.Unit.PX);
            img.getStyle().setWidth(15, Style.Unit.PX);
            img.getStyle().setVerticalAlign(Style.VerticalAlign.BOTTOM);
            img.setSrc(GWT.getModuleBaseURL() + "images/" + (sortDir ? "arrowup.png" : "arrowdown.png"));

            SpanElement span = Document.get().createSpanElement();
            span.getStyle().setWhiteSpace(Style.WhiteSpace.NORMAL);
            span.setInnerText(escapedCaption);

            renderedCaptionElement = span;

            div.appendChild(img);
            div.appendChild(span);
            
            th.appendChild(div);
        } else {
            div.getStyle().setWhiteSpace(Style.WhiteSpace.NORMAL);
            div.setInnerText(escapedCaption);
            renderedCaptionElement = div;
            th.appendChild(div);
        }

        if (notNull) {
            DivElement notNullSign = Document.get().createDivElement();
            notNullSign.addClassName("rightBottomCornerTriangle");
            notNullSign.addClassName("notNullCornerTriangle");
            th.appendChild(notNullSign);
        } else if (hasChangeAction) {
            DivElement changeActionSign = Document.get().createDivElement();
            changeActionSign.addClassName("rightBottomCornerTriangle");
            changeActionSign.addClassName("changeActionCornerTriangle");
            th.appendChild(changeActionSign);
        }

        setRendered(caption, sortDir);
    }

    private String getEscapedCaption() {
        return caption == null ? "" : EscapeUtils.unicodeEscape(caption);
    }

    @Override
    public void updateDom(TableCellElement th) {
        Boolean sortDir = table.getSortDirection(this);

        if (!nullEquals(sortDir, renderedSortDir)) {
            //пока не заморачиваемся с апдейтом DOMа при изменении сортировки... просто перерендериваем
            GwtClientUtils.removeAllChildren(th);
            renderDom(th);
        } else if (!nullEquals(this.caption, renderedCaption)) {

            String escapedCaption = getEscapedCaption();

            renderedCaptionElement.setInnerText(escapedCaption);
        }

        setRendered(caption, sortDir);
    }

    private void setRendered(String caption, Boolean sortDir) {
        renderedCaption = caption;
        renderedSortDir = sortDir;
    }

    private class ColumnResizeHelper implements NativePreviewHandler {
        private HandlerRegistration previewHandlerReg;

        private int initalMouseX;

        private Column leftColumn;
        private double leftInitialWidth;

        private double scaleWidth;
        private int scalePixelWidth;

        private Column[] rightColumns;
        private double[] rightInitialWidths;
        private double[] rightCoeffs;

        public ColumnResizeHelper(Column leftColumn, Column[] rightColumns, double[] rightScaleWidths, int initalMouseX, double scaleWidth, int scalePixelWidth) {
            this.leftColumn = leftColumn;
            this.rightColumns = rightColumns;
            this.initalMouseX = initalMouseX;
            this.scaleWidth = scaleWidth;
            this.scalePixelWidth = scalePixelWidth;

            leftInitialWidth = scaleWidth;

            double rightSum = 0.0;
            for (double rightScaleWidth : rightScaleWidths) {
                rightSum += rightScaleWidth;
            }
            rightInitialWidths = new double[rightColumns.length];
            rightCoeffs = new double[rightColumns.length];
            for (int i = 0; i < rightColumns.length; i++) {
                rightInitialWidths[i] = getColumnWidth(rightColumns[i]);
                rightCoeffs[i] = rightSum == 0.0 ? 0.0 : rightScaleWidths[i] / rightSum;
            }

            previewHandlerReg = Event.addNativePreviewHandler(this);
        }

        @Override
        public void onPreviewNativeEvent(NativePreviewEvent event) {
            NativeEvent nativeEvent = event.getNativeEvent();
            stopPropagation(nativeEvent);
            if (nativeEvent.getType().equals(MOUSEMOVE)) {
                int clientX = nativeEvent.getClientX();
                int tableLeft = table.getAbsoluteLeft();
                if (clientX >= tableLeft && clientX <= tableLeft + table.getOffsetWidth()) {
                    resizeHeaders(clientX);
                }
            } else if (nativeEvent.getType().equals(MOUSEUP)) {
                previewHandlerReg.removeHandler();
                resizeHelper = null;
            }
        }

        private void resizeHeaders(int clientX) {
            int dragX = clientX - initalMouseX;
            double dragColumnWidth = dragX * scaleWidth / scalePixelWidth;
            double newLeftWidth = leftInitialWidth + dragColumnWidth;

            if (table.getTableDataScroller().getMaximumHorizontalScrollPosition() > 0) {
                GPropertyDraw property = table.getProperty(leftColumn);
                int propertyMinWidth = property != null ? property.getMinimumPixelValueWidth(null) : 0;
                int propertyMaxWidth = property != null ? property.getMaximumPixelValueWidth() : Integer.MAX_VALUE;
                if (property == null || (newLeftWidth >= propertyMinWidth && newLeftWidth <= propertyMaxWidth)) {
                    table.setColumnWidth(leftColumn, newLeftWidth + "px");
                    if (table instanceof GGridTable) {
                        ((GGridTable) table).setUserWidth(property, (int) newLeftWidth);
                    }
                    table.onResize();
                }
            } else {
                if (newLeftWidth > 0) {
                    table.setColumnWidth(leftColumn, newLeftWidth + "px");
                    GPropertyDraw leftProperty = table.getProperty(leftColumn);
                    if (table instanceof GGridTable && leftProperty != null) {
                        ((GGridTable) table).setUserWidth(leftProperty, (int) newLeftWidth);
                    }

                    for (int i = 0; i < rightColumns.length; i++) {
                        double newWidth = rightInitialWidths[i] - (rightCoeffs[i] != 0.0 ? dragColumnWidth * rightCoeffs[i] : dragColumnWidth / rightCoeffs.length);
                        table.setColumnWidth(rightColumns[i], newWidth + "px");
                        GPropertyDraw property = table.getProperty(rightColumns[i]);
                        if (table instanceof GGridTable && property != null) {
                            ((GGridTable) table).setUserWidth(property, (int) newLeftWidth);
                        }
                    }
                    table.onResize();
                }
            }
        }
    }
}