// ============================================================================
//
// Talend Community Edition
//
// Copyright (C) 2006 Talend - www.talend.com
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
//
// ============================================================================
package org.talend.designer.mapper.ui.dnd;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.talend.commons.ui.swt.tableviewer.TableViewerCreator;
import org.talend.commons.ui.utils.TableUtils;
import org.talend.commons.ui.ws.WindowSystem;
import org.talend.core.model.action.IAction;
import org.talend.core.model.metadata.IMetadataColumn;
import org.talend.core.model.metadata.MetadataColumn;
import org.talend.core.ui.metadata.editor.MetadataTableEditorView;
import org.talend.designer.mapper.language.ILanguage;
import org.talend.designer.mapper.language.LanguageProvider;
import org.talend.designer.mapper.managers.MapperManager;
import org.talend.designer.mapper.managers.UIManager;
import org.talend.designer.mapper.model.tableentry.IColumnEntry;
import org.talend.designer.mapper.model.tableentry.ITableEntry;
import org.talend.designer.mapper.model.tableentry.TableEntryLocation;
import org.talend.designer.mapper.ui.visualmap.table.DataMapTableView;
import org.talend.designer.mapper.ui.visualmap.zone.Zone;
import org.talend.designer.mapper.ui.visualmap.zone.scrollable.TablesZoneView;

/**
 * DOC amaumont class global comment. Detailled comment <br/>
 * 
 * $Id$
 * 
 */
public class CompleteDropTargetListener extends DefaultDropTargetListener {

    private Table draggableTable;

    private boolean isIntersectAtPreviousDragOver;

    private Point lastDragPosition;

    /**
     * DOC amaumont CompleteDropTargetListener constructor comment.
     * 
     * @param mapperManager
     */
    public CompleteDropTargetListener(MapperManager mapperManager, Table draggableTable) {
        super(mapperManager);
        this.draggableTable = draggableTable;
    }

    public void dragEnter(DropTargetEvent event) {
        super.dragEnter(event);
        draggableTable.setFocus();
    }

    public void dragOver(DropTargetEvent event) {

        super.dragOver(event);

        // System.out.println("\n>>dragOver");

        DraggedData draggedData = TableEntriesTransfer.getInstance().getDraggedData();
        DropContextAnalyzer analyzer = analyzeDropTarget(event, draggedData);
        UIManager uiManager = mapperManager.getUiManager();
        DraggingInfosPopup draggingInfosPopup = uiManager.getDraggingInfosPopup();

        fillEvent(event, analyzer);
        InsertionIndicator insertionIndicator = retrieveInsertionIndicator();
        if (analyzer.isDropValid()) {

            draggingInfosPopup.setOutputToOutputMode(analyzer.isOutputToOutput());

            if (analyzer.isInputToInput()) {
                draggingInfosPopup.setExpressionContext(true);
                draggingInfosPopup.setInsertionEntryContext(false);
            } else if (analyzer.isCursorOverExpressionCell()) {
                draggingInfosPopup.setExpressionContext(true);
            } else if (analyzer.targetTableIsConstraintsTable()) {
                draggingInfosPopup.setExpressionContext(true);
            } else {
                draggingInfosPopup.setExpressionContext(false);
            }

            draggingInfosPopup.setMapOneToOneMode(analyzer.isMapOneToOneMode(), analyzer.isMapOneToOneAuthorized());
            Integer itemIndexWhereInsertFromPosition = getItemIndexFromPosition(new Point(event.x, event.y));
            if (analyzer.isMapOneToOneMode() && analyzer.isMapOneToOneAuthorized()) {
                int size = draggedData.getTransferableEntryList().size();
                if (itemIndexWhereInsertFromPosition != null) {
                    draggableTable.setSelection(itemIndexWhereInsertFromPosition, itemIndexWhereInsertFromPosition + size - 1);
                    if (!analyzer.targetTableIsConstraintsTable()
                            && itemIndexWhereInsertFromPosition + size - 1 >= draggableTable.getItemCount()) {
                        insertionIndicator.updatePosition(draggableTable, draggableTable.getItemCount());
                        insertionIndicator.setVisible(true);
                        draggingInfosPopup.setInsertionEntryContext(true);
                    } else {
                        insertionIndicator.setVisible(false);
                        draggingInfosPopup.setInsertionEntryContext(false);
                    }
                } else {
                    draggableTable.deselectAll();
                    insertionIndicator.setVisible(false);
                    draggingInfosPopup.setInsertionEntryContext(false);
                }
            } else {
                if (!analyzer.isTableSourceAndTargetAreSame()) {
                    draggableTable.deselectAll();
                }
                if (itemIndexWhereInsertFromPosition != null && !analyzer.isInsertionEntryMode()) {
                    draggableTable.setSelection(itemIndexWhereInsertFromPosition);
                }
                if (!analyzer.targetTableIsConstraintsTable()) {
                    updateInsertionIndicator(event);
                    insertionIndicator.setVisible(analyzer.isInsertionEntryMode());
                }
                draggingInfosPopup.setInsertionEntryContext(analyzer.isInsertionEntryMode());
            }
            draggingInfosPopup.setDropInvalid(false, false);
        } else {
            draggingInfosPopup.setMapOneToOneMode(false, false);
            draggingInfosPopup.setExpressionContext(false);
            draggingInfosPopup.setInsertionEntryContext(false);
            draggingInfosPopup.setDropInvalid(true, analyzer.isInvalidKeyPressed());
        }
    }

    private void configurePopupInfos(DropContextAnalyzer analyzer) {
        UIManager uiManager = mapperManager.getUiManager();
        DraggingInfosPopup draggingInfosPopup = uiManager.getDraggingInfosPopup();

        if (analyzer.isDropValid()) {

            draggingInfosPopup.setOutputToOutputMode(analyzer.isOutputToOutput());

            if (uiManager.isCtrlPressed()) {
                draggingInfosPopup.setOverwriteMode(true);
            } else {
                draggingInfosPopup.setOverwriteMode(false);
            }

            if (uiManager.isShiftPressed() && analyzer.isMapOneToOneAuthorized()) {
                draggingInfosPopup.setMapOneToOneMode(true, true);
            } else {
                draggingInfosPopup.setMapOneToOneMode(false, analyzer.isMapOneToOneAuthorized());
            }

            if (analyzer.isInputToInput()) {
                draggingInfosPopup.setExpressionContext(true);
                draggingInfosPopup.setInsertionEntryContext(false);
            } else if (analyzer.isCursorOverExpressionCell()) {
                draggingInfosPopup.setExpressionContext(true);
                draggingInfosPopup.setInsertionEntryContext(analyzer.isInsertionEntryMode());
            } else if (analyzer.targetTableIsConstraintsTable()) {
                draggingInfosPopup.setExpressionContext(true);
                draggingInfosPopup.setInsertionEntryContext(false);
            } else {
                draggingInfosPopup.setExpressionContext(false);
                draggingInfosPopup.setInsertionEntryContext(true);
            }
            draggingInfosPopup.setDropInvalid(false, false);
        } else {
            draggingInfosPopup.setExpressionContext(false);
            draggingInfosPopup.setMapOneToOneMode(false, false);
            draggingInfosPopup.setInsertionEntryContext(false);
            draggingInfosPopup.setDropInvalid(true, analyzer.isInvalidKeyPressed());
        }
    }

    private DropContextAnalyzer analyzeDropTarget(DropTargetEvent event, DraggedData draggedData) {
        DropContextAnalyzer analyzer = new DropContextAnalyzer(draggedData, event, mapperManager);
        return analyzer;
    }

    private void fillEvent(DropTargetEvent event, DropContextAnalyzer analyzer) {
        event.feedback = analyzer.getFeedback();
        event.detail = analyzer.getDetail();
    }

    /**
     * 
     * DOC amaumont Comment method "updateInsertionIndicator".
     * 
     * @param event
     * @return true if droppable
     */
    private void updateInsertionIndicator(DropTargetEvent event) {
        UIManager uiManager = mapperManager.getUiManager();
        DraggingInfosPopup draggingInfosPopup = uiManager.getDraggingInfosPopup();

        // ////////////////////////////////
        // to resolve graphical bug between popupInfos and InsertionIndicator
        InsertionIndicator insertionIndicator = retrieveInsertionIndicator();
        Rectangle popupBounds = draggingInfosPopup.getBounds();
        Point positionPopupFromMapperShellOrigin = draggingInfosPopup.getPositionFromMapperShellOrigin();
        Rectangle boundsPopupFromMapperShellOrigin = new Rectangle(positionPopupFromMapperShellOrigin.x,
                positionPopupFromMapperShellOrigin.y, popupBounds.width, popupBounds.height);
        boolean intersect = insertionIndicator.isLeftArrowMustBeRefreshed(boundsPopupFromMapperShellOrigin);

        Point eventPosition = new Point(event.x, event.y);
        // int itemIndexTarget = getItemIndexWhereInsertFromPosition(eventPosition);
        int itemIndexTarget = TableUtils.getItemIndexWhereInsertFromPosition(draggableTable, eventPosition);
        insertionIndicator.updatePosition(draggableTable, itemIndexTarget);

        if (WindowSystem.isGTK()) {
            if (insertionIndicator.isRightArrowVisible() && !eventPosition.equals(lastDragPosition)) {
                insertionIndicator.setRightArrowVisible(false);
                insertionIndicator.setLefArrowVisible(false);
            } else {
                insertionIndicator.setRightArrowVisible(insertionIndicator.isVisible());
                insertionIndicator.setLefArrowVisible(insertionIndicator.isVisible());
            }

        } else {
            if (isIntersectAtPreviousDragOver || intersect) {
                insertionIndicator.setLefArrowVisible(false);
                isIntersectAtPreviousDragOver = false;
            } else {
                insertionIndicator.setLefArrowVisible(insertionIndicator.isVisible());
            }
        }

        isIntersectAtPreviousDragOver = intersect;
        lastDragPosition = eventPosition;
    }

    private InsertionIndicator retrieveInsertionIndicator() {
        DataMapTableView dataMapTableViewTarget = mapperManager.retrieveDataMapTableView(draggableTable);
        TablesZoneView targetTablesZoneView = mapperManager.getUiManager().getTablesZoneView(dataMapTableViewTarget);
        InsertionIndicator insertionIndicator = targetTablesZoneView.getInsertionIndicator();
        return insertionIndicator;
    }

    public void dragLeave(DropTargetEvent event) {
        // System.out.println("\n>>dragLeave");
        // System.out.println(event);
        super.dragLeave(event);

        DraggedData draggedData = TableEntriesTransfer.getInstance().getDraggedData();
        DropContextAnalyzer analyzer = analyzeDropTarget(event, draggedData);

        UIManager uiManager = mapperManager.getUiManager();
        if (!analyzer.isTableSourceAndTargetAreSame()) {
            draggableTable.deselectAll();
        }
        retrieveInsertionIndicator().setVisible(false);
        DraggingInfosPopup draggingInfosPopup = uiManager.getDraggingInfosPopup();
        draggingInfosPopup.setExpressionContext(false);
        draggingInfosPopup.setInsertionEntryContext(false);
        uiManager.setDragging(false);
    }

    public void dragOperationChanged(DropTargetEvent event) {
        // System.out.println("\n>>dragOperationChanged");
        // showInfos(event);
        // super.dragOperationChanged(event);
        DraggedData draggedData = TableEntriesTransfer.getInstance().getDraggedData();
        detectPressedKeys(event);
        DropContextAnalyzer analyzer = analyzeDropTarget(event, draggedData);

        configurePopupInfos(analyzer);

        UIManager uiManager = mapperManager.getUiManager();
        DraggingInfosPopup draggingInfosPopup = uiManager.getDraggingInfosPopup();
        draggingInfosPopup.updateVisibleLabels();

        fillEvent(event, analyzer);
        mapperManager.getUiManager().setCurrentDragDetail(event.detail);
    }

    // private void showInfos(DropTargetEvent event) {
    // System.out.println(event);
    // System.out.println("event.feedback=" + event.feedback);
    // System.out.println("event.detail=" + event.detail);
    // System.out.println("event.operations=" + event.operations);
    //
    // System.out.println("DND.DROP_DEFAULT=" + DND.DROP_DEFAULT);
    // System.out.println("DND.DROP_COPY=" + DND.DROP_COPY);
    // System.out.println("DND.DROP_MOVE=" + DND.DROP_MOVE);
    // System.out.println("DND.DROP_LINK=" + DND.DROP_LINK);
    // System.out.println("DND.DROP_TARGET_MOVE=" + DND.DROP_TARGET_MOVE);
    // }
    //
    public void drop(DropTargetEvent event) {

        super.drop(event);

        retrieveInsertionIndicator().setVisible(false);

        UIManager uiManager = mapperManager.getUiManager();

        DraggedData draggedData = TableEntriesTransfer.getInstance().getDraggedData();
        DropContextAnalyzer analyzer = analyzeDropTarget(event, draggedData);

        // System.out.println("\n>>drop");
        // System.out.println(event);
        Point cursorPosition = new Point(event.x, event.y);
        // int startInsertAtThisIndex = getItemIndexWhereInsertFromPosition(cursorPosition);
        int startInsertAtThisIndex = TableUtils.getItemIndexWhereInsertFromPosition(draggableTable, cursorPosition);
        ILanguage currentLanguage = LanguageProvider.getCurrentLanguage();
        DataMapTableView dataMapTableViewTarget = mapperManager.retrieveDataMapTableView(draggableTable);
        Zone zoneTarget = dataMapTableViewTarget.getZone();

        uiManager.selectDataMapTableView(dataMapTableViewTarget);
        MetadataTableEditorView metadataEditorView = mapperManager.getUiManager().getMetadataEditorView(dataMapTableViewTarget.getZone());
        List<TransferableEntry> transferableEntryList = draggedData.getTransferableEntryList();
        int currentIndex = startInsertAtThisIndex;
        uiManager.clearLastCreatedInOutColumnEntries();

        draggableTable.deselectAll();

        ITableEntry currentEntryTarget = getEntryFromPosition(cursorPosition);

        ArrayList<String> columnsBeingAdded = new ArrayList<String>();
        ArrayList<ITableEntry> sourceEntriesOfEntriesBeingAdded = new ArrayList<ITableEntry>();
        ArrayList<IMetadataColumn> metadataColumnsBeingAdded = new ArrayList<IMetadataColumn>();

        boolean targetTableIsConstraintsTable = analyzer.targetTableIsConstraintsTable();
        boolean atLeastOneEntryInserted = false;

        boolean insertionEntryMode = analyzer.isInsertionEntryMode();
        boolean mapEachSourceToNextTargets = analyzer.isMapOneToOneMode();

        TableViewerCreator tableViewerCreatorTarget = null;
        if (targetTableIsConstraintsTable) {
            tableViewerCreatorTarget = dataMapTableViewTarget.getTableViewerCreatorForConstraints();
        } else {
            tableViewerCreatorTarget = dataMapTableViewTarget.getTableViewerCreatorForColumns();
        }

//        MetadataEditorEvent metadataEditorEvent = new MetadataEditorEvent(MetadataEditorEvent.TYPE.ADD);
        ITableEntry lastEntryTarget = null;
        for (TransferableEntry transferableEntry : transferableEntryList) {
            ITableEntry tableEntrySource = transferableEntry.getTableEntrySource();
            IMetadataColumn metadataColumnDragged = transferableEntry.getMetadataColumn();
            Zone zoneSourceEntry = transferableEntry.getZoneSourceEntry();

            TableEntryLocation tableEntryLocationTarget = new TableEntryLocation(dataMapTableViewTarget.getDataMapTable().getName(),
                    tableEntrySource.getName());

            if (zoneSourceEntry == Zone.INPUTS && zoneTarget == Zone.INPUTS
                    && tableEntrySource.getParentName().equals(tableEntryLocationTarget.tableName)) {

                continue;

            } else if (currentEntryTarget != null && !insertionEntryMode) {

                boolean overwrite = (lastEntryTarget != currentEntryTarget && analyzer.isOverwriteExpression());
                modifyExistingExpression(currentLanguage, currentEntryTarget, tableEntrySource, overwrite, zoneSourceEntry);
                uiManager.parseExpression(currentEntryTarget.getExpression(), currentEntryTarget, false, true, true);

            } else {
                String columnName = transferableEntry.getTableEntrySource().getName();
                tableEntryLocationTarget = mapperManager.findUniqueLocation(tableEntryLocationTarget, columnsBeingAdded
                        .toArray(new String[0]));
                columnName = tableEntryLocationTarget.columnName;
                if (currentEntryTarget == null && analyzer.isMapOneToOneMode()) {
                    currentIndex = tableViewerCreatorTarget.getInputList().size();
                }
                if (zoneSourceEntry == Zone.INPUTS && zoneTarget == Zone.VARS || zoneSourceEntry == Zone.VARS && zoneTarget == Zone.VARS) {
                    currentIndex = insertNewVarEntry(currentLanguage, dataMapTableViewTarget, currentIndex, tableEntrySource, columnName);
                    atLeastOneEntryInserted = true;

                } else if (zoneSourceEntry == Zone.VARS && zoneTarget == Zone.OUTPUTS) {
                    insertNewOutputEntryFromVarEntry(sourceEntriesOfEntriesBeingAdded,
                            metadataColumnsBeingAdded,
//                            metadataEditorEvent, 
                            tableEntrySource, columnName);
                    atLeastOneEntryInserted = true;

                } else if (zoneSourceEntry == Zone.INPUTS && zoneTarget != Zone.VARS) {
                    insertNewInOutEntryFromInputEntry(sourceEntriesOfEntriesBeingAdded, 
                            metadataColumnsBeingAdded,
//                            metadataEditorEvent, 
                            tableEntrySource,
                            metadataColumnDragged, columnName);
                    atLeastOneEntryInserted = true;

                } else if (zoneSourceEntry == Zone.OUTPUTS && zoneTarget == Zone.VARS) {
                    // nothing
                } else if (zoneSourceEntry == Zone.OUTPUTS && zoneTarget == Zone.OUTPUTS) {

                    insertOutpuEntryCopyToOtherOutput(sourceEntriesOfEntriesBeingAdded, 
                            metadataColumnsBeingAdded,
//                            metadataEditorEvent, 
                            tableEntrySource,
                            metadataColumnDragged, columnName);
                    atLeastOneEntryInserted = true;

                } else {
                    // throw new IllegalStateException("Drop case not found !");
                }
                columnsBeingAdded.add(columnName);
            }

            lastEntryTarget = currentEntryTarget;
            if (mapEachSourceToNextTargets && currentEntryTarget != null) {
                currentEntryTarget = getNextEntryTarget(currentEntryTarget, tableViewerCreatorTarget);
            }

        } // for TransferableEntry transferableEntry : transferableEntryList

        if (!atLeastOneEntryInserted) {
            tableViewerCreatorTarget.getTableViewer().refresh();
        } else {
            updateExpressionsOfInsertedEntries(currentLanguage, metadataEditorView, currentIndex, sourceEntriesOfEntriesBeingAdded,
                    targetTableIsConstraintsTable, tableViewerCreatorTarget
//                    , metadataEditorEvent
                    , metadataColumnsBeingAdded
                    );
        }
        dataMapTableViewTarget.resizeAtExpandedSize();
        dataMapTableViewTarget.unselectAllEntries();

        uiManager.refreshBackground(true, false);
        if (metadataEditorView != null && !targetTableIsConstraintsTable) {
            metadataEditorView.getTableViewerCreator().getTableViewer().refresh();
        }

        uiManager.setDragging(false);
    }

    /**
     * DOC amaumont Comment method "updateExpressionsOfInsertedEntries".
     * 
     * @param uiManager
     * @param currentLanguage
     * @param metadataEditorView
     * @param currentIndex
     * @param sourceEntriesOfEntriesBeingAdded
     * @param targetTableIsConstraintsTable
     * @param tableViewerCreatorTarget
     * @param metadataColumnsBeingAdded 
     * @param metadataEditorEvent
     */
    @SuppressWarnings("unchecked")
    private void updateExpressionsOfInsertedEntries(ILanguage currentLanguage, MetadataTableEditorView metadataEditorView,
            int currentIndex, ArrayList<ITableEntry> sourceEntriesOfEntriesBeingAdded, boolean targetTableIsConstraintsTable,
            TableViewerCreator tableViewerCreatorTarget, ArrayList<IMetadataColumn> metadataColumnsBeingAdded
//            , MetadataEditorEvent metadataEditorEvent
            ) {
        UIManager uiManager = mapperManager.getUiManager();

        if (metadataEditorView != null && !targetTableIsConstraintsTable) {
            metadataEditorView.getMetadataTableEditor().addAll(currentIndex, metadataColumnsBeingAdded);
            
//            metadataEditorEvent.indexStartInsert = currentIndex;
//            IAction action = MetadataEditorActionFactory.getInstance().getAction(metadataEditorView, metadataEditorEvent);
//            action.run(metadataEditorEvent);

            List<IColumnEntry> lastCreatedTableEntries = uiManager.getLastCreatedInOutColumnEntries();
            for (int i = 0; i < lastCreatedTableEntries.size(); i++) {
                ITableEntry tableEntrySource = sourceEntriesOfEntriesBeingAdded.get(i);
                ITableEntry dataMapTableEntry = lastCreatedTableEntries.get(i);
                Zone zoneSource = mapperManager.retrieveAbstractDataMapTableView(tableEntrySource.getParent()).getZone();
                String location = null;
                if (zoneSource == Zone.OUTPUTS) {
                    location = tableEntrySource.getExpression();
                } else {
                    location = currentLanguage.getLocation(tableEntrySource.getParentName(), tableEntrySource.getName());
                }

                if (location != null) {
                    dataMapTableEntry.setExpression(location + " ");
                }
            }
        }

        tableViewerCreatorTarget.getTableViewer().refresh();

        List<ITableEntry> refreshedTableEntriesList = tableViewerCreatorTarget.getInputList();
        for (ITableEntry tableEntry : refreshedTableEntriesList) {
            uiManager.parseExpression(tableEntry.getExpression(), tableEntry, false, true, false);
        }
    }

    @SuppressWarnings("unchecked")
    private ITableEntry getNextEntryTarget(ITableEntry currentEntryTarget, TableViewerCreator tableViewerCreatorTarget) {
        // mapperManager.get
        // currentEntryTarget.getParent()
        if (currentEntryTarget == null) {
            throw new IllegalArgumentException("currentEntryTarget should'nt be null");
        }
        List<ITableEntry> tableEntries = tableViewerCreatorTarget.getInputList();
        int indexCurrentEntryTarget = tableEntries.indexOf(currentEntryTarget);
        if (indexCurrentEntryTarget + 1 >= tableEntries.size()) {
            return null;
        } else {
            return tableEntries.get(indexCurrentEntryTarget + 1);
        }
    }

    private void insertNewInOutEntryFromInputEntry(ArrayList<ITableEntry> sources, 
            ArrayList<IMetadataColumn> metadataColumnsBeingAdded, 
//            MetadataEditorEvent metadataEditorEvent,
            ITableEntry tableEntrySource, IMetadataColumn metadataColumnDragged, String columnName) {
        MetadataColumn metadataColumn = new MetadataColumn(metadataColumnDragged);
        metadataColumn.setLabel(columnName);
        metadataColumnsBeingAdded.add(metadataColumn);
//        metadataEditorEvent.entries.add(metadataColumn);
        sources.add(tableEntrySource);
    }

    private void insertOutpuEntryCopyToOtherOutput(ArrayList<ITableEntry> sources, 
            ArrayList<IMetadataColumn> metadataColumnsBeingAdded, 
//            MetadataEditorEvent metadataEditorEvent,
            ITableEntry tableEntrySource, IMetadataColumn metadataColumnDragged, String columnName) {
        MetadataColumn metadataColumn = new MetadataColumn(metadataColumnDragged);
        metadataColumn.setLabel(columnName);
        metadataColumnsBeingAdded.add(metadataColumn);
//        metadataEditorEvent.entries.add(metadataColumn);
        sources.add(tableEntrySource);
    }

    private void insertNewOutputEntryFromVarEntry(ArrayList<ITableEntry> sources, 
ArrayList<IMetadataColumn> metadataColumnsBeingAdded, 
//            MetadataEditorEvent metadataEditorEvent,
            ITableEntry tableEntrySource, String columnName) {
        MetadataColumn metadataColumn = new MetadataColumn();
        metadataColumn.setLabel(columnName);
        metadataColumnsBeingAdded.add(metadataColumn);
//        metadataEditorEvent.entries.add(metadataColumn);
        sources.add(tableEntrySource);
    }

    private int insertNewVarEntry(ILanguage currentLanguage, DataMapTableView dataMapTableViewTarget, int currentIndex,
            ITableEntry tableEntrySource, String columnName) {
        ITableEntry dataMapTableEntry;
        dataMapTableEntry = mapperManager.addNewColumnEntry(dataMapTableViewTarget, columnName, currentIndex++);
        String location = currentLanguage.getLocation(tableEntrySource.getParentName(), tableEntrySource.getName());
        dataMapTableEntry.setExpression(location + " ");
        return currentIndex;
    }

    private void modifyExistingExpression(ILanguage currentLanguage, ITableEntry entryTarget, ITableEntry tableEntrySource,
            boolean overwriteExpression, Zone zoneSourceEntry) {
        String expression = null;
        if (zoneSourceEntry == Zone.OUTPUTS) {
            expression = tableEntrySource.getExpression();
        } else {
            expression = currentLanguage.getLocation(tableEntrySource.getParentName(), tableEntrySource.getName());
        }
        if (expression == null) {
            return;
        }
        if (overwriteExpression) {
            entryTarget.setExpression(expression + "  ");
        } else {
            String currentTargetExpression = entryTarget.getExpression();
            if (currentTargetExpression == null) {
                currentTargetExpression = "";
            }
            String space = "";
            boolean isEmpty = "".equals(currentTargetExpression.trim());
            if (currentTargetExpression.endsWith("  ")) {
                space = "";
            } else if (!isEmpty && currentTargetExpression.endsWith(" ")) {
                space = " ";
            } else if (!isEmpty) {
                space = "  ";
            }
            entryTarget.setExpression(currentTargetExpression + space + expression + " ");
        }
    }

    public void dropAccept(DropTargetEvent event) {
        // System.out.println("\n>>dropAccept");
        // System.out.println(event);
        super.dropAccept(event);

    }

    /**
     * 
     * DOC amaumont Comment method "getItemIndexFromPosition".
     * 
     * @param cursorPosition
     * @return
     */
    private Integer getItemIndexFromPosition(Point cursorPosition) {
        TableItem[] tableItems = draggableTable.getItems();
        TableItem tableItemBehindCursor = getTableItemFromPosition(cursorPosition);
        if (tableItemBehindCursor != null) {
            for (int i = 0; i < tableItems.length; i++) {
                if (tableItems[i] == tableItemBehindCursor) {
                    return i;
                }
            }
        }
        return null;
    }

    /**
     * 
     * DOC amaumont Comment method "getTableItemFromPosition".
     * 
     * @param cursorPosition
     * @return
     */
    private TableItem getTableItemFromPosition(Point cursorPosition) {
        Point pointCursor = draggableTable.toControl(cursorPosition.x, cursorPosition.y);
        if (WindowSystem.isGTK()) {
            pointCursor.y -= draggableTable.getHeaderHeight();
        }
        return draggableTable.getItem(pointCursor);
    }

    /**
     * 
     * DOC amaumont Comment method "getEntryFromPosition".
     * 
     * @param cursorPosition
     * @return
     */
    private ITableEntry getEntryFromPosition(Point cursorPosition) {
        TableItem tableItemBehindCursor = getTableItemFromPosition(cursorPosition);
        if (tableItemBehindCursor != null) {
            return (ITableEntry) tableItemBehindCursor.getData();
        } else {
            return null;
        }
    }

    /**
     * 
     * DOC amaumont Comment method "getItemIndexWhereInsertFromPosition".
     * 
     * @param cursorPosition
     * @return
     */
    private int getItemIndexWhereInsertFromPosition(Point cursorPosition) {
        int startInsertAtThisIndex = 0;
        Point pointCursor = draggableTable.toControl(cursorPosition.x, cursorPosition.y);
        TableItem[] tableItems = draggableTable.getItems();
        TableItem tableItemBehindCursor = getTableItemFromPosition(cursorPosition);
        if (tableItemBehindCursor != null) {
            for (int i = 0; i < tableItems.length; i++) {
                if (tableItems[i] == tableItemBehindCursor) {
                    Rectangle boundsItem = tableItemBehindCursor.getBounds();
                    startInsertAtThisIndex = i;
                    if (pointCursor.y > boundsItem.y + draggableTable.getItemHeight() / 2
                            + (WindowSystem.isGTK() ? draggableTable.getHeaderHeight() : 0)) {
                        startInsertAtThisIndex = i + 1;
                    }
                    break;
                }
            }
        } else if (pointCursor.y < draggableTable.getHeaderHeight()) {
            startInsertAtThisIndex = 0;
        } else {
            startInsertAtThisIndex = tableItems.length;
        }
        return startInsertAtThisIndex;
    }

}
