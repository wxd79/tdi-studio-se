// ============================================================================
//
// Copyright (C) 2006-2009 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.repository.model.migration;

import java.util.Arrays;
import java.util.Date;
import java.util.GregorianCalendar;

import org.talend.commons.exception.ExceptionHandler;
import org.talend.commons.exception.PersistenceException;
import org.talend.core.language.ECodeLanguage;
import org.talend.core.model.components.ComponentUtilities;
import org.talend.core.model.components.ModifyComponentsAction;
import org.talend.core.model.components.conversions.IComponentConversion;
import org.talend.core.model.components.filters.IComponentFilter;
import org.talend.core.model.components.filters.NameComponentFilter;
import org.talend.core.model.migration.AbstractJobMigrationTask;
import org.talend.core.model.properties.Item;
import org.talend.designer.core.model.utils.emf.talendfile.ElementParameterType;
import org.talend.designer.core.model.utils.emf.talendfile.NodeType;
import org.talend.designer.core.model.utils.emf.talendfile.ProcessType;

/**
 * DOC Administrator class global comment. Detailled comment
 */
public class MigrationTaskForIssue6577 extends AbstractJobMigrationTask {

    @Override
    public ExecutionResult execute(Item item) {
        ProcessType processType = getProcessType(item);
        if (getProject().getLanguage() != ECodeLanguage.JAVA || processType == null) {
            return ExecutionResult.NOTHING_TO_DO;
        }

        IComponentFilter filter = new NameComponentFilter("tReplace");
        try {
            ModifyComponentsAction.searchAndModify(item, processType, filter, Arrays
                    .<IComponentConversion> asList(new IComponentConversion() {

                        public void transform(NodeType node) {
                            ElementParameterType advancedCheck = ComponentUtilities.getNodeProperty(node, "ADVANCED_MODE");
                            ElementParameterType simpleCheck = ComponentUtilities.getNodeProperty(node, "SIMPLE_MODE");
                            ComponentUtilities.addNodeProperty(node, "MODE", "CLOSED_LIST");
                            ComponentUtilities.addNodeProperty(node, "SUPPORT_REGEX", "CHECK");
                            ElementParameterType modeList = ComponentUtilities.getNodeProperty(node, "MODE");
                            ElementParameterType supportRegex = ComponentUtilities.getNodeProperty(node, "SUPPORT_REGEX");
                            if (simpleCheck != null && simpleCheck.getValue().equals("true")) {
                                modeList.setValue("SIMPLE_MODE");
                                supportRegex.setValue("true");
                            }
                            if (advancedCheck != null && advancedCheck.getValue().equals("true")) {
                                modeList.setValue("ADVANCED_MODE");
                            }
                        }
                    }));
        } catch (PersistenceException e) {
            ExceptionHandler.process(e);
            return ExecutionResult.FAILURE;
        }

        return ExecutionResult.SUCCESS_WITH_ALERT;

    }

    public Date getOrder() {
        GregorianCalendar gc = new GregorianCalendar(2008, 8, 5, 16, 0, 0);
        return gc.getTime();
    }
}
