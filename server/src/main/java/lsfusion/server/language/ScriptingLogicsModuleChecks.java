package lsfusion.server.language;

import lsfusion.server.base.version.Version;
import lsfusion.server.data.expr.formula.SQLSyntaxType;
import lsfusion.server.data.expr.query.PartitionType;
import lsfusion.server.data.type.ConcatenateType;
import lsfusion.server.data.type.Type;
import lsfusion.server.language.ScriptingLogicsModule.GroupingType;
import lsfusion.server.language.ScriptingLogicsModule.LPWithParams;
import lsfusion.server.language.ScriptingLogicsModule.TypedParameter;
import lsfusion.server.language.action.LA;
import lsfusion.server.language.metacode.MetaCodeFragment;
import lsfusion.server.language.property.LP;
import lsfusion.server.language.property.oraction.LAP;
import lsfusion.server.logics.LogicsModule;
import lsfusion.server.logics.action.flow.ListCaseAction;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.classes.data.DataClass;
import lsfusion.server.logics.classes.data.file.FileClass;
import lsfusion.server.logics.classes.data.integral.IntegralClass;
import lsfusion.server.logics.classes.user.ConcreteCustomClass;
import lsfusion.server.logics.classes.user.CustomClass;
import lsfusion.server.logics.classes.user.set.ResolveClassSet;
import lsfusion.server.logics.form.interactive.design.ComponentView;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.struct.group.AbstractGroup;
import lsfusion.server.logics.navigator.NavigatorElement;
import lsfusion.server.logics.navigator.window.AbstractWindow;
import lsfusion.server.logics.property.JoinProperty;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.cases.CaseUnionProperty;
import lsfusion.server.logics.property.classes.infer.ClassType;
import lsfusion.server.logics.property.data.DataProperty;
import lsfusion.server.logics.property.data.SessionDataProperty;
import lsfusion.server.physics.dev.i18n.LocalizedString;
import lsfusion.server.physics.dev.id.name.PropertyCanonicalNameUtils;
import lsfusion.server.physics.dev.id.resolve.*;
import lsfusion.server.physics.exec.db.table.ImplementTable;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static lsfusion.server.physics.dev.id.resolve.SignatureMatcher.isClassesSoftCompatible;

public class ScriptingLogicsModuleChecks {
    private ScriptingLogicsModule LM;
    private ScriptingErrorLog errLog;
    private ScriptParser parser;
    
    public ScriptingLogicsModuleChecks(ScriptingLogicsModule LM) {
        this.LM = LM;
        this.errLog = LM.getErrLog();
        this.parser = LM.getParser();
    } 
    
    public void checkGroup(AbstractGroup group, String name) throws ScriptingErrorLog.SemanticErrorException {
        if (group == null) {
            errLog.emitGroupNotFoundError(parser, name);
        }
    }

    public void checkClass(ValueClass cls, String name) throws ScriptingErrorLog.SemanticErrorException {
        if (cls == null) {
            errLog.emitClassNotFoundError(parser, name);
        }
    }

    public void checkComponent(ComponentView component, String name) throws ScriptingErrorLog.SemanticErrorException {
        if (component == null) {
            errLog.emitComponentNotFoundError(parser, name);
        }
    }

    public void checkProperty(LP lp, String name, List<ResolveClassSet> signature) throws ScriptingErrorLog.SemanticErrorException {
        if (lp == null) {
            errLog.emitPropertyNotFoundError(parser, PropertyCanonicalNameUtils.createName(null, name, signature));
        }
    }

    public void checkAction(LA lp, String name, List<ResolveClassSet> signature) throws ScriptingErrorLog.SemanticErrorException {
        if (lp == null) {
            errLog.emitActionNotFoundError(parser, PropertyCanonicalNameUtils.createName(null, name, signature));
        }
    }

    public void checkModule(LogicsModule module, String name) throws ScriptingErrorLog.SemanticErrorException {
        if (module == null) {
            errLog.emitModuleNotFoundError(parser, name);
        }
    }

    public void checkNamespace(String namespaceName) throws ScriptingErrorLog.SemanticErrorException {
        if (!LM.getNamespaceToModules().containsKey(namespaceName)) {
            errLog.emitNamespaceNotFoundError(parser, namespaceName);
        }
    }

    public void checkWindow(AbstractWindow window, String name) throws ScriptingErrorLog.SemanticErrorException {
        if (window == null) {
            errLog.emitWindowNotFoundError(parser, name);
        }
    }

    public void checkNavigatorElement(NavigatorElement element, String name) throws ScriptingErrorLog.SemanticErrorException {
        if (element == null) {
            errLog.emitNavigatorElementNotFoundError(parser, name);
        }
    }

    public void checkTable(ImplementTable table, String name) throws ScriptingErrorLog.SemanticErrorException {
        if (table == null) {
            errLog.emitTableNotFoundError(parser, name);
        }
    }

    public void checkForm(FormEntity form, String name) throws ScriptingErrorLog.SemanticErrorException {
        if (form == null) {
            errLog.emitFormNotFoundError(parser, name);
        }
    }

    public void checkMetaCodeFragment(MetaCodeFragment code, String name) throws ScriptingErrorLog.SemanticErrorException {
        if (code == null) {
            errLog.emitMetaCodeFragmentNotFoundError(parser, name);
        }
    }

    public void checkParamCount(LAP mainProp, int paramCount) throws ScriptingErrorLog.SemanticErrorException {
        if (mainProp.property.interfaces.size() != paramCount) {
            errLog.emitParamCountError(parser, mainProp, paramCount);
        }
    }

    public void checkPropertyValue(LP<?> property, Map<Property, String> alwaysNullProperties) {
        if (!property.property.checkAlwaysNull(false) && !alwaysNullProperties.containsKey(property.property)) {
            String path = parser.getCurrentScriptPath(LM.getName(), parser.getCurrentParserLineNumber(), "\n\t\t\t");
            String location = path + ":" + (parser.getCurrentParser().input.LT(1).getCharPositionInLine() + 1);
            alwaysNullProperties.put(property.property, location);
        }
    }

    public void checkDuplicateClass(String className) throws ScriptingErrorLog.SemanticErrorException {
        checkDuplicateElement(className, "class", new ModuleClassFinder());
    }

    public void checkDuplicateGroup(String groupName) throws ScriptingErrorLog.SemanticErrorException {
        checkDuplicateElement(groupName, "group", new ModuleGroupFinder());
    }

    public void checkDuplicateWindow(String windowName) throws ScriptingErrorLog.SemanticErrorException {
        checkDuplicateElement(windowName, "window", new ModuleWindowFinder());
    }

    public void checkDuplicateNavigatorElement(String navigatorElementName) throws ScriptingErrorLog.SemanticErrorException {
        checkDuplicateElement(navigatorElementName, "navigator element", new ModuleNavigatorElementFinder());
    }

    public void checkDuplicateForm(String formName) throws ScriptingErrorLog.SemanticErrorException {
        checkDuplicateElement(formName, "form", new ModuleFormFinder());
    }

    public void checkDuplicateMetaCodeFragment(String metacodeName, int paramCnt) throws ScriptingErrorLog.SemanticErrorException {
        checkDuplicateElement(metacodeName, "meta code", new ModuleMetaCodeFragmentFinder(), paramCnt);
    }

    public void checkDuplicateTable(String tableName) throws ScriptingErrorLog.SemanticErrorException {
        checkDuplicateElement(tableName, "table", new ModuleTableFinder());
    }

    public <E, P> void checkDuplicateElement(String elementName, String type, ModuleFinder<E, P> finder) throws ScriptingErrorLog.SemanticErrorException {
        checkDuplicateElement(elementName, type, finder, null);
    }

    public void checkDuplicateProperty(String propName, List<ResolveClassSet> signature) throws ScriptingErrorLog.SemanticErrorException {
        checkDuplicateElement(propName, "property", new ModuleEqualLPFinder(true), signature);
    }

    public void checkDuplicateAction(String propName, List<ResolveClassSet> signature) throws ScriptingErrorLog.SemanticErrorException {
        checkDuplicateElement(propName, "action", new ModuleEqualLAFinder(), signature);
    }

    public <E, P> void checkDuplicateElement(String elementName, String type, ModuleFinder<E, P> finder, P param) throws ScriptingErrorLog.SemanticErrorException {
        NamespaceElementFinder<E, P> nsFinder = new NamespaceElementFinder<>(finder, LM.getRequiredModules(LM.getNamespace()));
        List<NamespaceElementFinder.FoundItem<E>> foundItems = nsFinder.findInNamespace(LM.getNamespace(), elementName, param);
        if (!foundItems.isEmpty()) {
            errLog.emitAlreadyDefinedError(parser, type, elementName, foundItems);
        }
    }
    
    public void checkPropertyTypes(List<LPWithParams> properties, String errMsgPropType) throws ScriptingErrorLog.SemanticErrorException {
        LP<?> lp1 = properties.get(0).getLP();
        if(lp1 == null)
            return;
        Property prop1 = lp1.property;
        for (int i = 1; i < properties.size(); i++) {
            LP<?> lp2 = properties.get(i).getLP();
            if(lp2 == null)
                return;
            Property prop2 = lp2.property;
            if (prop1.getType() != null && prop2.getType() != null && prop1.getType().getCompatible(prop2.getType()) == null) {
                errLog.emitIncompatibleTypes(parser, errMsgPropType);
            }
        }
    }

    public void checkStaticClassConstraints(boolean isAbstract, List<String> instNames, List<LocalizedString> instCaptions) throws ScriptingErrorLog.SemanticErrorException {
        assert instCaptions.size() == instNames.size();
        if (isAbstract && !instNames.isEmpty()) {
            errLog.emitAbstractClassInstancesDefError(parser);
        }

        Set<String> names = new HashSet<>();
        for (String name : instNames) {
            if (names.contains(name)) {
                errLog.emitAlreadyDefinedError(parser, "instance", name);
            }
            names.add(name);
        }
    }

    public void checkClassParents(List<String> parents) throws ScriptingErrorLog.SemanticErrorException {
        Set<ValueClass> parentsSet = new HashSet<>();
        for (String parentName : parents) {
            ValueClass valueClass = LM.findClass(parentName);
            if (!(valueClass instanceof CustomClass)) {
                errLog.emitBuiltInClassAsParentError(parser, parentName);
            }

            if (parentsSet.contains(valueClass)) {
                errLog.emitDuplicateClassParentError(parser, parentName);
            }
            parentsSet.add(valueClass);
        }
    }


    public void checkCIInExpr(ScriptingLogicsModule.LPContextIndependent lcp) throws ScriptingErrorLog.SemanticErrorException {
        if (lcp != null) {
            errLog.emitCIInExpr(parser);
        }
    }

    public void checkFormulaClass(ValueClass cls) throws ScriptingErrorLog.SemanticErrorException {
        if (!(cls instanceof DataClass)) {
            errLog.emitFormulaReturnClassError(parser);
        }
    }

    public void checkFormDataClass(ValueClass cls) throws ScriptingErrorLog.SemanticErrorException {
        if (!(cls instanceof DataClass)) {
            errLog.emitFormDataClassError(parser);
        }
    }

    public void checkInputDataClass(ValueClass cls) throws ScriptingErrorLog.SemanticErrorException {
        if (!(cls instanceof DataClass)) {
            errLog.emitInputDataClassError(parser);
        }
    }

    public void checkChangeClassActionClass(ValueClass cls) throws ScriptingErrorLog.SemanticErrorException {
        if (!(cls instanceof ConcreteCustomClass)) {
            errLog.emitChangeClassActionClassError(parser);
        }
    }

    public void checkSingleImplementation(List<SQLSyntaxType> types) throws ScriptingErrorLog.SemanticErrorException {
        Set<SQLSyntaxType> foundTypes = new HashSet<>();
        for (SQLSyntaxType type : types) {
            if (!foundTypes.add(type)) {
                errLog.emitFormulaMultipleImplementationError(parser, type);
            }
        }
    }

    public void checkFormulaParameters(Set<Integer> params) throws ScriptingErrorLog.SemanticErrorException {
        for (int param : params) {
            if (param == 0 || param > params.size()) {
                errLog.emitFormulaParamIndexError(parser, param, params.size());
            }
        }
    }

    public void checkNamedParams(LAP property, List<String> namedParams) throws ScriptingErrorLog.SemanticErrorException {
        if (property.property.interfaces.size() != namedParams.size() && !namedParams.isEmpty()) {
            errLog.emitNamedParamsError(parser);
        }
    }

    public void checkParamsClasses(List<TypedParameter> params, List<ResolveClassSet> signature) throws ScriptingErrorLog.SemanticErrorException {
        if (!params.isEmpty()) {
            assert params.size() == signature.size();
            for (int i = 0; i < params.size(); ++i) {
                ValueClass paramClass = params.get(i).cls;
                String paramName = params.get(i).paramName;
                if (paramClass != null && !isClassesSoftCompatible(paramClass.getResolveSet(), signature.get(i))) {
                    errLog.emitWrongPropertyParameterError(parser, paramName, paramClass.toString(), signature.get(i).toString());
                }
            }
        }
    }
    
    public <T> void checkDistinctParameters(List<T> params) throws ScriptingErrorLog.SemanticErrorException {
        Set<T> paramsSet = new HashSet<>(params);
        if (paramsSet.size() < params.size()) {
            errLog.emitDistinctParamNamesError(parser);
        }
    }

    public void checkDistinctParametersList(List<LPWithParams> lps) throws ScriptingErrorLog.SemanticErrorException {
        for (LPWithParams lp : lps) {
            checkDistinctParameters(lp.usedParams);
        }
    }

    public void checkMetaCodeParamCount(MetaCodeFragment code, int paramCnt) throws ScriptingErrorLog.SemanticErrorException {
        if (code.parameters.size() != paramCnt) {
            errLog.emitParamCountError(parser, code.parameters.size(), paramCnt);
        }
    }

    public void checkGPropOrderConsistence(GroupingType type, int orderParamsCnt) throws ScriptingErrorLog.SemanticErrorException {
        if (type != GroupingType.CONCAT && type != GroupingType.LAST && orderParamsCnt > 0) {
            errLog.emitRedundantOrderGPropError(parser, type);
        }
    }

    public void checkGPropAggregateConsistence(GroupingType type, int aggrParamsCnt) throws ScriptingErrorLog.SemanticErrorException {
        if (type != GroupingType.CONCAT && aggrParamsCnt > 1) {
            errLog.emitMultipleAggrGPropError(parser, type);
        }
        if (type == GroupingType.CONCAT && aggrParamsCnt != 2) {
            errLog.emitConcatAggrGPropError(parser);
        }
    }

    public void checkGPropSumConstraints(GroupingType type, ScriptingLogicsModule.LPWithParams mainProp) throws ScriptingErrorLog.SemanticErrorException {
        if (type == GroupingType.SUM && mainProp.getLP() != null) {
            if (!(mainProp.getLP().property.getValueClass(ClassType.valuePolicy).getType() instanceof IntegralClass)) {
                errLog.emitNonIntegralSumArgumentError(parser);
            }
        }
    }

    public void checkGPropAggrConstraints(GroupingType type, List<LPWithParams> mainProps, List<ScriptingLogicsModule.LPWithParams> groupProps) throws ScriptingErrorLog.SemanticErrorException {
        if (type == GroupingType.AGGR || type == GroupingType.NAGGR) {
            if (mainProps.get(0).getLP() != null) {
                errLog.emitNonObjectAggrGPropError(parser);
            }
        }
    }

    public void checkGPropWhereConsistence(GroupingType type, LPWithParams where) throws ScriptingErrorLog.SemanticErrorException {
        if (type != GroupingType.AGGR && type != GroupingType.NAGGR && type != GroupingType.LAST && where != null) {
            errLog.emitWhereGPropError(parser, type);
        }
    }

    public void checkNavigatorAction(LA<?> property) throws ScriptingErrorLog.SemanticErrorException {
        if (property.listInterfaces.size() > 0) {
            errLog.emitWrongNavigatorAction(parser);
        }
    }

    public void checkAddActionsClass(ValueClass cls) throws ScriptingErrorLog.SemanticErrorException {
        if (!(cls instanceof CustomClass)) {
            errLog.emitAddActionsClassError(parser);
        }
    }

    public void checkAggrClass(ValueClass cls) throws ScriptingErrorLog.SemanticErrorException {
        if (!(cls instanceof CustomClass)) {
            errLog.emitAggrClassError(parser);
        }
    }

    public void checkCustomClass(ValueClass cls) throws ScriptingErrorLog.SemanticErrorException {
        if (!(cls instanceof CustomClass)) {
            errLog.emitCustomClassExpectedError(parser);
        }
    }

    public void checkSessionProperty(LAP property) throws ScriptingErrorLog.SemanticErrorException {
        if (!(property.property instanceof SessionDataProperty)) {
            errLog.emitNotSessionOrLocalPropertyError(parser);
        }
    }

    public void checkForActionConstraints(boolean isRecursive, List<Integer> oldContext, List<Integer> newContext) throws ScriptingErrorLog.SemanticErrorException {
        if (!isRecursive && oldContext.size() == newContext.size()) {
            errLog.emitForActionSameContextError(parser);
        }
    }

    public void checkRecursionContext(List<String> context, List<Integer> usedParams) throws ScriptingErrorLog.SemanticErrorException {
        for (String param : context) {
            if (param.startsWith("$")) {
                int indexPlain = context.indexOf(param.substring(1));
                if (indexPlain < 0) {
                    errLog.emitParamNotFoundError(parser, param.substring(1));
                }
                if (!usedParams.contains(indexPlain)) {
                    errLog.emitParameterNotUsedInRecursionError(parser, param.substring(1));
                }
            }
        }
    }

    public void checkNecessaryProperty(LPWithParams property) throws ScriptingErrorLog.SemanticErrorException {
        if (property.getLP() == null) {
            errLog.emitNecessaryPropertyError(parser);
        }
    }

    public void checkIndexNecessaryProperty(List<LPWithParams> lps) throws ScriptingErrorLog.SemanticErrorException {
        boolean hasProperty = false;
        for (LPWithParams lp : lps) {
            if (lp.getLP() != null) {
                hasProperty = true;
                break;
            }
        }
        if (!hasProperty) {
            errLog.emitIndexWithoutPropertyError(parser);
        }
    }

    public void checkStoredProperties(List<LPWithParams> lps) throws ScriptingErrorLog.SemanticErrorException {
        ImplementTable table = null;
        String firstPropertyName = null;
        for (ScriptingLogicsModule.LPWithParams lp : lps) {
            if (lp.getLP() != null) {
                Property<?> property = lp.getLP().property;
                String name = property.getName();
                if (!property.isStored()) {
                    errLog.emitShouldBeStoredError(parser, name);
                }
                if (table == null) {
                    table = property.mapTable.table;
                    firstPropertyName = name;
                } else if (table != property.mapTable.table) {
                    errLog.emitIndexPropertiesDifferentTablesError(parser, firstPropertyName, name);
                }
            }
        }
    }

    public void checkIndexNumberOfParameters(int paramsCount, List<LPWithParams> lps) throws ScriptingErrorLog.SemanticErrorException {
        int paramsInProp = -1;
        for (LPWithParams lp : lps) {
            if (lp.getLP() != null) {
                if (paramsInProp == -1) {
                    paramsInProp = lp.usedParams.size();
                } else if (lp.usedParams.size() != paramsInProp){
                    errLog.emitIndexPropertiesNonEqualParamsCountError(parser);
                }
            }
        }
        if (paramsCount != paramsInProp) {
            errLog.emitIndexParametersError(parser);
        }
    }

    public void checkDeconcatenateIndex(LPWithParams property, int index) throws ScriptingErrorLog.SemanticErrorException {
        Type propType = property.getLP().property.getType();
        if (propType instanceof ConcatenateType) {
            int concatParts = ((ConcatenateType) propType).getPartsCount();
            if (index <= 0 || index > concatParts) {
                errLog.emitDeconcatIndexError(parser, index, concatParts);
            }
        } else {
            errLog.emitDeconcatError(parser);
        }
    }

    public void checkPartitionWindowConsistence(PartitionType partitionType, boolean useLast) throws ScriptingErrorLog.SemanticErrorException {
        if (!useLast && (partitionType != PartitionType.SUM && partitionType != PartitionType.PREVIOUS)) {
            errLog.emitIllegalWindowPartitionError(parser);
        }
    }

    public void checkPartitionUngroupConsistence(LAP ungroupProp, int groupPropCnt) throws ScriptingErrorLog.SemanticErrorException {
        if (ungroupProp != null && ungroupProp.property.interfaces.size() != groupPropCnt) {
            errLog.emitUngroupParamsCntPartitionError(parser, groupPropCnt);
        }
    }

//    public void checkClassWhere(LP<?> property, String name) {
//        ClassWhere<Integer> classWhere = property.getClassWhere(ClassType.signaturePolicy);
//        boolean needWarning = false;
//        if (classWhere.wheres.length > 1) {
//            needWarning = true;
//        } else {
//            AbstractClassWhere.And<Integer> where = classWhere.wheres[0];
//            for (int i = 0; i < where.size(); ++i) {
//                ResolveClassSet acSet = where.getValue(i);
//                if (acSet instanceof UpClassSet && ((UpClassSet)acSet).wheres.length > 1 ||
//                    acSet instanceof OrObjectClassSet && ((OrObjectClassSet)acSet).up.wheres.length > 1) {
//
//                    needWarning = true;
//                    break;
//                }
//            }
//        }
//        if (needWarning) {
//            warningList.add(" Property " + name + " has class where " + classWhere);
//        }
//    }

    public void checkAbstractProperty(LP property, String propName) throws ScriptingErrorLog.SemanticErrorException {
        if (!(property.property instanceof CaseUnionProperty && ((CaseUnionProperty)property.property).isAbstract())) {
            errLog.emitNotAbstractPropertyError(parser, propName);
        }
    }

    public void checkAbstractAction(LA action, String actionName) throws ScriptingErrorLog.SemanticErrorException {
        if (!(action.property instanceof ListCaseAction && ((ListCaseAction)action.property).isAbstract())) {
            errLog.emitNotAbstractActionError(parser, actionName);
        }
    }

    public void checkNoInline(boolean innerPD) throws ScriptingErrorLog.SemanticErrorException {
        if (innerPD) {
            errLog.emitNoInline(parser);
        }
    }

    public void checkEventNoParameters(LAP property) throws ScriptingErrorLog.SemanticErrorException {
        if (property.property.interfaces.size() > 0) {
            errLog.emitEventNoParametersError(parser);
        }
    }

    public void checkChangeClassWhere(boolean contextExtended, ScriptingLogicsModule.LPWithParams param, LPWithParams where, List<TypedParameter> newContext) throws ScriptingErrorLog.SemanticErrorException {
        if (contextExtended && (where == null || !where.usedParams.contains(param.usedParams.get(0)))) {
            errLog.emitChangeClassWhereError(parser, newContext.get(newContext.size() - 1).paramName);
        }
    }

    public void checkAddObjTOParams(int contextSize, List<LPWithParams> toPropMapping) throws ScriptingErrorLog.SemanticErrorException {
        if (toPropMapping != null) {
            for (LPWithParams param : toPropMapping) {
                if (param.usedParams.get(0) < contextSize) {
                    errLog.emitAddObjToPropertyError(parser);
                }
            }
        }
    }

    public void checkAbstractTypes(boolean isCase, boolean implIsCase) throws ScriptingErrorLog.SemanticErrorException {
        if (isCase && !implIsCase) {
            errLog.emitAbstractCaseImplError(parser);
        }
        if (!isCase && implIsCase) {
            errLog.emitAbstractNonCaseImplError(parser);
        }
    }

    public void checkRange(String valueType, int value, int lbound, int rbound) throws ScriptingErrorLog.SemanticErrorException {
        if (value < lbound || value > rbound) {
            errLog.emitOutOfRangeError(parser, valueType, lbound, rbound);
        }
    }

    public void checkImplementIsNotMain(LAP mainProp, LAP implProp) throws ScriptingErrorLog.SemanticErrorException {
        if (mainProp == implProp) {
            errLog.emitRecursiveImplementError(parser);
        }
    }

    public void checkNavigatorElementName(String name) throws ScriptingErrorLog.SemanticErrorException {
        if (name == null) {
            errLog.emitNavigatorElementFolderNameError(parser);
        }
    }

    public void checkNavigatorElementMoveOperation(NavigatorElement element, NavigatorElement parentElement, 
                                                   NavigatorElement anchorElement, boolean isEditOperation, Version version) throws ScriptingErrorLog.SemanticErrorException {
        if (parentElement.isLeafElement()) {
            errLog.emitIllegalParentNavigatorElement(parser, parentElement.getCanonicalName());
        }

        // если редактирование существующего элемента, и происходит перемещение элемента, то оно должно происходить только внутри своего уровня 
        if (isEditOperation && !parentElement.equals(element.getNFParent(version))) {
            errLog.emitIllegalNavigatorElementMove(parser, element.getCanonicalName(), parentElement.getCanonicalName());
        }

        if (anchorElement != null && !parentElement.equals(anchorElement.getNFParent(version))) {
            errLog.emitIllegalInsertBeforeAfterElement(parser, element.getCanonicalName(), parentElement.getCanonicalName(), anchorElement.getCanonicalName());
        }

        if (element.isAncestorOf(parentElement, version)) {
            errLog.emitIllegalAddNavigatorToSubnavigator(parser, element.getCanonicalName(), parentElement.getCanonicalName());
        }
    } 
    
    public void checkAssignProperty(LPWithParams fromProperty, LPWithParams toProperty) throws ScriptingErrorLog.SemanticErrorException {
        LP<?> toLCP = toProperty.getLP();
        if (!(toLCP.property instanceof DataProperty || toLCP.property instanceof CaseUnionProperty || toLCP.property instanceof JoinProperty)) { // joinproperty только с неповторяющимися параметрами
            errLog.emitOnlyDataCasePropertyIsAllowedError(parser, toLCP.property.getName());
        }

        if (fromProperty.getLP() != null && fromProperty.getLP().property.getType() != null &&
                toLCP.property.getType().getCompatible(fromProperty.getLP().property.getType()) == null) {
            errLog.emitIncompatibleTypes(parser, "ASSIGN");
        }
    }

    public void checkImportFromFileExpression(LPWithParams params) throws ScriptingErrorLog.SemanticErrorException {
        if (params.getLP() != null && !(params.getLP().property.getValueClass(ClassType.valuePolicy).getType() instanceof FileClass)) {
            errLog.emitImportFromWrongClassError(parser);    
        }
    }

    public void checkComparisonCompatibility(LPWithParams leftProp, LPWithParams rightProp, List<TypedParameter> context) throws ScriptingErrorLog.SemanticErrorException {
        if (rightProp != null) {
            ValueClass leftClass = getValueClass(leftProp, context);
            ValueClass rightClass = getValueClass(rightProp, context);
            if (leftClass != null && rightClass != null && !isClassesSoftCompatible(leftClass.getResolveSet(), rightClass.getResolveSet())) {
                errLog.emitRelationalOperatorClassCommpatibilityError(parser, leftClass.getParsedName(), rightClass.getParsedName());
            }
        }
    }
    
    private ValueClass getValueClass(LPWithParams prop, List<TypedParameter> context) {
        if (prop.getLP() == null) {
            return context.get(prop.usedParams.get(0)).cls;
        } else {
            return prop.getLP().property.getValueClass(ClassType.valuePolicy);
        }
    }

    public void checkBooleanUsage(boolean value) throws ScriptingErrorLog.SemanticErrorException {
        if (!value) {
            errLog.emitUseNullInsteadOfFalseError(parser);
        }
    }
}
