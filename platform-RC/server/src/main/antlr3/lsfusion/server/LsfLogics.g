grammar LsfLogics;

@header {
	package lsfusion.server;

	import lsfusion.base.OrderedMap;
	import lsfusion.interop.ClassViewType;
	import lsfusion.interop.PropertyEditType;
	import lsfusion.interop.form.layout.ContainerType;
	import lsfusion.interop.form.layout.FlexAlignment;
	import lsfusion.interop.form.layout.Alignment;
	import lsfusion.interop.form.ServerResponse;
	import lsfusion.interop.FormEventType;
	import lsfusion.interop.FormStaticType;
	import lsfusion.interop.FormPrintType;
	import lsfusion.interop.FormExportType;
	import lsfusion.interop.ModalityType;
	import lsfusion.interop.WindowFormType;
	import lsfusion.interop.ReflectionPropertyType;
	import lsfusion.server.form.instance.FormSessionScope;
	import lsfusion.server.data.expr.query.PartitionType;
	import lsfusion.server.form.entity.*;
	import lsfusion.server.form.navigator.NavigatorElement;
	import lsfusion.server.form.view.ComponentView;
	import lsfusion.server.form.view.GroupObjectView;
	import lsfusion.server.form.view.PropertyDrawView;
	import lsfusion.server.classes.sets.ResolveClassSet;
	import lsfusion.server.classes.DataClass;
	import lsfusion.server.classes.CustomClass;
	import lsfusion.server.classes.ValueClass;
	import lsfusion.server.session.LocalNestedType;
	import lsfusion.server.logics.i18n.LocalizedString;
	import lsfusion.server.logics.mutables.Version;
	import lsfusion.server.logics.linear.LP;
	import lsfusion.server.logics.linear.LAP;
	import lsfusion.server.logics.linear.LCP;
	import lsfusion.server.logics.property.Cycle;
	import lsfusion.server.logics.property.ImportSourceFormat;
	import lsfusion.server.logics.scripted.*;
	import lsfusion.server.logics.scripted.ScriptingLogicsModule.WindowType;
	import lsfusion.server.logics.scripted.ScriptingLogicsModule.InsertPosition;
	import lsfusion.server.logics.scripted.ScriptingLogicsModule.GroupingType;
	import lsfusion.server.logics.scripted.ScriptingLogicsModule.LPWithParams;
	import lsfusion.server.logics.scripted.ScriptingLogicsModule.TypedParameter;
	import lsfusion.server.logics.scripted.ScriptingLogicsModule.PropertyUsage;
	import lsfusion.server.logics.scripted.ScriptingLogicsModule.AbstractPropertyUsage;
	import lsfusion.server.logics.scripted.ScriptingLogicsModule.LPUsage;
	import lsfusion.server.logics.scripted.ScriptingLogicsModule.FormActionProps;
	import lsfusion.server.logics.scripted.ScriptingLogicsModule.NavigatorElementOptions;
	import lsfusion.server.logics.scripted.ScriptingFormEntity.RegularFilterInfo;
	import lsfusion.server.mail.SendEmailActionProperty.FormStorageType;
	import lsfusion.server.mail.AttachmentFormat;
	import lsfusion.server.logics.property.actions.file.FileActionType;
	import lsfusion.server.logics.property.actions.flow.Inline;
	import lsfusion.server.logics.property.actions.SystemEvent;
	import lsfusion.server.logics.property.Event;
	import lsfusion.server.logics.property.actions.flow.ListCaseActionProperty;
	import lsfusion.server.logics.property.CaseUnionProperty;
	import lsfusion.server.logics.property.IncrementType;
	import lsfusion.server.data.expr.formula.SQLSyntaxType;
    import lsfusion.server.logics.property.actions.ChangeEvent;
	import lsfusion.server.logics.property.BooleanDebug;
	import lsfusion.server.logics.property.PropertyFollowsDebug;
	import lsfusion.server.logics.debug.DebugInfo;
	import lsfusion.server.logics.property.PropertySettings;
	import javax.mail.Message;

	import lsfusion.server.form.entity.GroupObjectProp;

	import java.util.*;
	import java.awt.*;
	import org.antlr.runtime.BitSet;

	import static java.util.Arrays.asList;
	import static lsfusion.server.logics.scripted.ScriptingLogicsModule.WindowType.*;
}

@lexer::header { 
	package lsfusion.server; 
	import lsfusion.server.logics.scripted.ScriptingLogicsModule;
	import lsfusion.server.logics.scripted.ScriptParser;
}

@lexer::members {
	public ScriptingLogicsModule self;
	public ScriptParser.State parseState;
	
	@Override
	public void emitErrorMessage(String msg) {
		if (parseState == ScriptParser.State.INIT) { 
			self.getErrLog().write(msg + "\n");
		}
	}
	
	@Override
	public String getErrorMessage(RecognitionException e, String[] tokenNames) {
		return self.getErrLog().getErrorMessage(this, super.getErrorMessage(e, tokenNames), e);
	}
	
	@Override
	public void displayRecognitionError(String[] tokenNames, RecognitionException e) {
		self.getErrLog().displayRecognitionError(this, self.getParser(), "error", tokenNames, e);
	}
	
	private boolean ahead(String text) {
		for(int i = 0; i < text.length(); i++) {
			if(input.LA(i + 1) != text.charAt(i)) {
				return false;
			}
		}
		return true;
	}	
}

@members {
	public ScriptingLogicsModule self;
	public ScriptParser.State parseState;
	
	private boolean insideRecursion = false;
	
	public boolean inParseState(ScriptParser.State parseState) {
		return this.parseState == parseState;
	}

	public boolean inPreParseState() {
		return inParseState(ScriptParser.State.PRE);
	}

	public boolean inInitParseState() {
		return inParseState(ScriptParser.State.INIT); 
	}

	public boolean inGroupParseState() {
		return inParseState(ScriptParser.State.GROUP);
	}

	public boolean inClassParseState() {
		return inParseState(ScriptParser.State.CLASS);
	}

	public boolean inPropParseState() {
		return inParseState(ScriptParser.State.PROP);
	}

	public boolean inTableParseState() {
		return inParseState(ScriptParser.State.TABLE);
	}

	public boolean inIndexParseState() {
		return inParseState(ScriptParser.State.INDEX);
	}

	public DebugInfo.DebugPoint getCurrentDebugPoint() {
		return getCurrentDebugPoint(false);
	}

	public DebugInfo.DebugPoint getCurrentDebugPoint(boolean previous) {
		return self.getParser().getGlobalDebugPoint(self.getName(), previous);
	}

	public DebugInfo.DebugPoint getEventDebugPoint() {
		return getCurrentDebugPoint();
	}

	public void setObjectProperty(Object propertyReceiver, String propertyName, Object propertyValue) throws ScriptingErrorLog.SemanticErrorException {
		if (inPropParseState()) {
			$designStatement::design.setObjectProperty(propertyReceiver, propertyName, propertyValue);
		}
	}

	public List<GroupObjectEntity> getGroupObjectsList(List<String> ids, Version version) throws ScriptingErrorLog.SemanticErrorException {
		if (inPropParseState()) {
			return $formStatement::form.getGroupObjectsList(ids, version);
		}
		return null;
	}

	public TypedParameter TP(String className, String paramName) throws ScriptingErrorLog.SemanticErrorException {
		return self.new TypedParameter(className, paramName);
	}

	@Override
	public void emitErrorMessage(String msg) {
		if (parseState == ScriptParser.State.INIT) { 
			self.getErrLog().write(msg + "\n");
		}
	}

	@Override
	public String getErrorMessage(RecognitionException e, String[] tokenNames) {
		return self.getErrLog().getErrorMessage(this, super.getErrorMessage(e, tokenNames), e);
	}

	@Override
	public void displayRecognitionError(String[] tokenNames, RecognitionException e) {
		self.getErrLog().displayRecognitionError(this, self.getParser(), "error", tokenNames, e);
	}
}

@rulecatch {
	catch(RecognitionException re) {
		if (re instanceof ScriptingErrorLog.SemanticErrorException) {
			throw re;
		} else {
			reportError(re);
			recover(input,re);
		}
	}
}

script	
	:	moduleHeader 
		statements 
		EOF
	;

statements
	:	statement*
	;

moduleHeader
@init {
	List<String> requiredModules = new ArrayList<String>();
	List<String> namespacePriority = new ArrayList<String>();
	String namespaceName = null;
}
@after {
	if (inPreParseState()) {
		self.initScriptingModule($name.text, namespaceName, requiredModules, namespacePriority);
	} else if (inInitParseState()) {
		self.initModulesAndNamespaces(requiredModules, namespacePriority);
	}
}
	:	'MODULE' name=ID ';'
		('REQUIRE' list=nonEmptyIdList ';' { requiredModules = $list.ids; })? 
		('PRIORITY' list=nonEmptyIdList ';' { namespacePriority = $list.ids; })? 
		('NAMESPACE' nname=ID ';' { namespaceName = $nname.text; })?
	;


statement
	:	(	classStatement
		|	extendClassStatement
		|	groupStatement
		|	propertyStatement
		|	overrideStatement
		|	constraintStatement
		|	followsStatement
		|	writeWhenStatement
		|	eventStatement
		|	showDepStatement
		|	globalEventStatement
		|	aspectStatement
		|	tableStatement
		|	loggableStatement
		|	indexStatement
		|	formStatement
		|	designStatement
		|	windowStatement
		|	navigatorStatement
		|	metaCodeDeclarationStatement
		|	metaCodeStatement 
		|	emptyStatement
		)
	;

metaCodeParsingStatement  // metacode parsing rule
	:	'META' ID '(' idList ')'
		statements
		'END'
	;

////////////////////////////////////////////////////////////////////////////////
////////////////////////////////// CLASS STATEMENT /////////////////////////////
////////////////////////////////////////////////////////////////////////////////


classStatement 
@init {
	List<String> classParents = new ArrayList<String>();
	boolean isAbstract = false;
	boolean isNative = false;
	boolean isComplex = false;
	DebugInfo.DebugPoint point = getCurrentDebugPoint(); 
}
@after {
	if (inClassParseState()) {
	    if (!isNative)
		    self.addScriptedClass($nameCaption.name, $nameCaption.caption, isAbstract, $classData.names, $classData.captions, $classData.parents, isComplex, point);
	}
}
	:	'CLASS'
		('ABSTRACT' {isAbstract = true;} | 'NATIVE' {isNative = true;})?
		('COMPLEX' { isComplex = true; })?
		nameCaption=simpleNameWithCaption
		classData=classInstancesAndParents
	;

extendClassStatement
@after {
	if (inClassParseState()) {
		self.extendClass($className.sid, $classData.names, $classData.captions, $classData.parents);
	}
}
	:	'EXTEND' 'CLASS' 
		className=compoundID 
		classData=classInstancesAndParents 
	;

classInstancesAndParents returns [List<String> names, List<LocalizedString> captions, List<String> parents] 
@init {
	$parents = new ArrayList<String>();
	$names = new ArrayList<String>();
	$captions = new ArrayList<LocalizedString>();
}
	:	(
			'{'
				(firstInstData=simpleNameWithCaption { $names.add($firstInstData.name); $captions.add($firstInstData.caption); }
				(',' nextInstData=simpleNameWithCaption { $names.add($nextInstData.name); $captions.add($nextInstData.caption); })*)?
			'}'
			(clist=classParentsList ';' { $parents = $clist.list; })? 	
		|
			(clist=classParentsList { $parents = $clist.list; })? ';'
		)
	; 

classParentsList returns [List<String> list] 
	:	':' parentList=nonEmptyClassIdList { $list = $parentList.ids; }
	; 

////////////////////////////////////////////////////////////////////////////////
////////////////////////////////// GROUP STATEMENT /////////////////////////////
////////////////////////////////////////////////////////////////////////////////

groupStatement
@init {
	String parent = null;
}
@after {
	if (inGroupParseState()) {
		self.addScriptedGroup($groupNameCaption.name, $groupNameCaption.caption, parent);
	}
}
	:	'GROUP' groupNameCaption=simpleNameWithCaption
		(':' parentName=compoundID { parent = $parentName.sid; })?
		';'
	;


////////////////////////////////////////////////////////////////////////////////
/////////////////////////////////// FORM STATEMENT /////////////////////////////
////////////////////////////////////////////////////////////////////////////////

formStatement
scope {
	ScriptingFormEntity form;
}
@init {
	boolean initialDeclaration = false;
	DebugInfo.DebugPoint point = getCurrentDebugPoint();
}
@after {
	if (inPropParseState() && initialDeclaration) {
		self.finalizeScriptedForm($formStatement::form);
	}
}
	:	(	declaration=formDeclaration { $formStatement::form = $declaration.form; initialDeclaration = true; if(inPropParseState()) self.addScriptedForm($formStatement::form, point); }
		|	extDecl=extendingFormDeclaration { $formStatement::form = $extDecl.form; }
		)
		(	formGroupObjectsList
		|	formTreeGroupObjectList
		|	formFiltersList
		|	formPropertiesList
		|	formHintsList
		|	formEventsList
		|	filterGroupDeclaration
		|	extendFilterGroupDeclaration
		|	formOrderByList
		|	dialogFormDeclaration
		|	editFormDeclaration
		|	reportFilesDeclaration
		)*
		';'
	;

dialogFormDeclaration
	:	'LIST' cid=classId 'OBJECT' oid=ID
		{
			if (inPropParseState()) {
				$formStatement::form.setAsDialogForm($cid.sid, $oid.text, self.getVersion());
			}
		}
	;

editFormDeclaration
	:	'EDIT' cid=classId 'OBJECT' oid=ID
		{
			if (inPropParseState()) {
				$formStatement::form.setAsEditForm($cid.sid, $oid.text, self.getVersion());
			}
		}
	;
	
reportFilesDeclaration
	:	'REPORTFILES' reportPath (',' reportPath)*
	;
	
reportPath
@init {
	GroupObjectEntity groupObject = null;
	PropertyUsage propUsage = null;
	List<String> mapping = null;
}
@after {
	if (inPropParseState()) {
		$formStatement::form.setReportPath(groupObject, propUsage, mapping);	
	}
}
	:	(
			'TOP' 
		| 	go = formGroupObjectEntity { groupObject = $go.groupObject; }
		) 
		prop = formMappedProperty { propUsage = $prop.propUsage; mapping = $prop.mapping; }
	;

formDeclaration returns [ScriptingFormEntity form]
@init {
	ModalityType modalityType = null;
	int autoRefresh = 0;
	String image = null;
	String title = null;
	DebugInfo.DebugPoint point = getCurrentDebugPoint();
}
@after {
	if (inPropParseState()) {
		$form = self.createScriptedForm($formNameCaption.name, $formNameCaption.caption, point, image, modalityType, autoRefresh);
	}
}
	:	'FORM' 
		formNameCaption=simpleNameWithCaption
		(	('IMAGE' img=stringLiteral { image = $img.val; })
		|	('AUTOREFRESH' refresh=intLiteral { autoRefresh = $refresh.val; })
		)*
	;


extendingFormDeclaration returns [ScriptingFormEntity form]
@after {
	if (inPropParseState()) {
		$form = self.getFormForExtending($formName.sid);
	}
}
	:	'EXTEND' 'FORM' formName=compoundID
	;

formGroupObjectsList
@init {
	List<ScriptingGroupObject> groups = new ArrayList<ScriptingGroupObject>();
}
@after {
	if (inPropParseState()) {
		$formStatement::form.addScriptingGroupObjects(groups, self.getVersion());
	}
}
	:	'OBJECTS'
		groupElement=formGroupObjectDeclaration { groups.add($groupElement.groupObject); }
		(',' groupElement=formGroupObjectDeclaration { groups.add($groupElement.groupObject); })*
	;

formTreeGroupObjectList
@init {
	String treeSID = null;
	List<ScriptingGroupObject> groups = new ArrayList<ScriptingGroupObject>();
	List<List<PropertyUsage>> properties = new ArrayList<List<PropertyUsage>>();
}
@after {
	if (inPropParseState()) {
		$formStatement::form.addScriptingTreeGroupObject(treeSID, groups, properties, self.getVersion());
	}
}
	:	'TREE'
		(id = ID { treeSID = $id.text; })?
		groupElement=formTreeGroupObjectDeclaration { groups.add($groupElement.groupObject); properties.add($groupElement.properties); }
		(',' groupElement=formTreeGroupObjectDeclaration { groups.add($groupElement.groupObject); properties.add($groupElement.properties); })*
	;

formGroupObjectDeclaration returns [ScriptingGroupObject groupObject]
	:	object=formCommonGroupObject { $groupObject = $object.groupObject; } formGroupObjectOptions[$groupObject]		
	; 

formGroupObjectOptions[ScriptingGroupObject groupObject]
	:	(	viewType=formGroupObjectViewType { $groupObject.setViewType($viewType.type); }
		|	initViewType=formGroupObjectInitViewType { $groupObject.setInitType($initViewType.isInitType); }
		|	pageSize=formGroupObjectPageSize { $groupObject.setPageSize($pageSize.value); }
		|	update=formGroupObjectUpdate { $groupObject.setUpdateType($update.updateType); }
		|	relative=formGroupObjectRelativePosition { $groupObject.setNeighbourGroupObject($relative.groupObject, $relative.isRightNeighbour); }
		)*
	;

formTreeGroupObjectDeclaration returns [ScriptingGroupObject groupObject, List<PropertyUsage> properties]
	:	(object=formCommonGroupObject { $groupObject = $object.groupObject; })
		(parent=treeGroupParentDeclaration { $properties = $parent.properties; })?
	; 

treeGroupParentDeclaration returns [List<PropertyUsage> properties = new ArrayList<PropertyUsage>()]
	:	'PARENT'
		(	id = propertyUsage { $properties.add($id.propUsage); }
		|	'('
				list=nonEmptyPropertyUsageList { $properties.addAll($list.propUsages); }
			')'
		)		
	;

formCommonGroupObject returns [ScriptingGroupObject groupObject]
	:	sdecl=formSingleGroupObjectDeclaration
		{
			$groupObject = new ScriptingGroupObject(null, asList($sdecl.name), asList($sdecl.className), asList($sdecl.caption), asList($sdecl.event));
		}
	|	mdecl=formMultiGroupObjectDeclaration
		{
			$groupObject = new ScriptingGroupObject($mdecl.groupName, $mdecl.objectNames, $mdecl.classNames, $mdecl.captions, $mdecl.events);
		}
	;

formGroupObjectViewType returns [ClassViewType type]
	:
		viewType=classViewType { $type = $viewType.type; }
	;

formGroupObjectInitViewType returns [boolean isInitType]
	: 	('INIT' {$isInitType = true;} | 'FIXED' {$isInitType = false;})
	;

classViewType returns [ClassViewType type]
	: 	('PANEL' {$type = ClassViewType.PANEL;} | 'GRID' {$type = ClassViewType.GRID;} | 'TOOLBAR' {$type = ClassViewType.TOOLBAR;} | 'LIST' {$type = ClassViewType.GRID;} | 'SINGLE' {$type = ClassViewType.PANEL;} )
	;

formGroupObjectPageSize returns [Integer value = null]
	:	'PAGESIZE' size=intLiteral { $value = $size.val; }
	;
	
formGroupObjectRelativePosition returns [GroupObjectEntity groupObject, boolean isRightNeighbour]
	:	'AFTER' go=formGroupObjectEntity { $groupObject = $go.groupObject; $isRightNeighbour = true; }
	|	'BEFORE' go=formGroupObjectEntity { $groupObject = $go.groupObject; $isRightNeighbour = false; }
	;

formGroupObjectUpdate returns [UpdateType updateType]
@init {
}
	:	'FIRST' { $updateType = UpdateType.FIRST; }
	|	'LAST' { $updateType = UpdateType.LAST; }
	|   'PREV' { $updateType = UpdateType.PREV; }
	;

formSingleGroupObjectDeclaration returns [String name, String className, LocalizedString caption, ActionPropertyObjectEntity event] 
	:	foDecl=formObjectDeclaration { $name = $foDecl.name; $className = $foDecl.className; $caption = $foDecl.caption; $event = $foDecl.event; }
	;

formMultiGroupObjectDeclaration returns [String groupName, List<String> objectNames, List<String> classNames, List<LocalizedString> captions, List<ActionPropertyObjectEntity> events]
@init {
	$objectNames = new ArrayList<String>();
	$classNames = new ArrayList<String>();
	$captions = new ArrayList<LocalizedString>();
	$events = new ArrayList<ActionPropertyObjectEntity>();
}
	:	(gname=ID { $groupName = $gname.text; } '=')?
		'('
			objDecl=formObjectDeclaration { $objectNames.add($objDecl.name); $classNames.add($objDecl.className); $captions.add($objDecl.caption); $events.add($objDecl.event); }
			(',' objDecl=formObjectDeclaration { $objectNames.add($objDecl.name); $classNames.add($objDecl.className); $captions.add($objDecl.caption); $events.add($objDecl.event); })+
		')'
	;


formObjectDeclaration returns [String name, String className, LocalizedString caption, ActionPropertyObjectEntity event]
	:	(objectName=ID { $name = $objectName.text; } '=')?
		id=classId { $className = $id.sid; }
		(c=localizedStringLiteral { $caption = $c.val; })?
		('ON' 'CHANGE' faprop=formActionPropertyObject { $event = $faprop.action; })?
	; 
	
formPropertiesList
@init {
	List<? extends AbstractPropertyUsage> properties = new ArrayList<>();
	List<String> aliases = new ArrayList<>();
	List<LocalizedString> captions = new ArrayList<>();	
	List<List<String>> mapping = new ArrayList<>();
	List<DebugInfo.DebugPoint> points = new ArrayList<>();
	FormPropertyOptions commonOptions = null;
	List<FormPropertyOptions> options = new ArrayList<>();
}
@after {
	if (inPropParseState()) {
		$formStatement::form.addScriptedPropertyDraws(properties, aliases, captions, mapping, commonOptions, options, self.getVersion(), points);
	}
}
	:	'PROPERTIES' '(' objects=idList ')' opts=formPropertyOptionsList list=formPropertyUList
		{
			commonOptions = $opts.options;
			properties = $list.properties;
			aliases = $list.aliases;
			captions = $list.captions;
			mapping = Collections.nCopies(properties.size(), $objects.ids);
			options = $list.options;
			points = $list.points;
		}
	|	'PROPERTIES' opts=formPropertyOptionsList mappedList=formMappedPropertiesList
		{
			commonOptions = $opts.options;
			properties = $mappedList.properties;
			aliases = $mappedList.aliases;
			captions = $mappedList.captions;
			mapping = $mappedList.mapping;
			options = $mappedList.options;
			points = $mappedList.points;
		}
	;	

// ???????????????????????? ?????? ???????????????? ?? ?????????????????? =pE -> (a=)?pe | pe ???????????????? ?????????????? lookahead, ?????? pe ???????????? SHOWIF pe pe, ???????????????????? ?? ? ?????? ?????????? formPropertyOptionsList ???????????????????????? ???? pe ?? ???????????? formMappedProperty | pe ?????????? ???????? formMappedProperty lookahead'???? 
formPropertyOptionsList returns [FormPropertyOptions options]
@init {
	$options = new FormPropertyOptions();
}
	:	(	editType = propertyEditTypeLiteral { $options.setEditType($editType.val); }
		|	'HINTNOUPDATE' { $options.setHintNoUpdate(true); }
		|	'HINTTABLE' { $options.setHintTable(true); }
        |   (('NEWSESSION' | 'NESTEDSESSION' { $options.setNested(true); } ) { $options.setNewSession(true); })
		|	'OPTIMISTICASYNC' { $options.setOptimisticAsync(true); }
		|	'COLUMNS' (columnsName=stringLiteral)? '(' ids=nonEmptyIdList ')' { $options.setColumns($columnsName.text, getGroupObjectsList($ids.ids, self.getVersion())); }
		|	'SHOWIF' propObj=formCalcPropertyObject { $options.setShowIf($propObj.property); }
		|	'READONLYIF' propObj=formCalcPropertyObject { $options.setReadOnlyIf($propObj.property); }
		|	'BACKGROUND' propObj=formCalcPropertyObject { $options.setBackground($propObj.property); }
		|	'FOREGROUND' propObj=formCalcPropertyObject { $options.setForeground($propObj.property); }
		|	'HEADER' propObj=formCalcPropertyObject { $options.setHeader($propObj.property); }
		|	'FOOTER' propObj=formCalcPropertyObject { $options.setFooter($propObj.property); }
		|	'FORCE'? viewType=classViewType { $options.setForceViewType($viewType.type); }
		|	'TODRAW' toDraw=formGroupObjectEntity { $options.setToDraw($toDraw.groupObject); }
		|	'BEFORE' pdraw=formPropertyDraw { $options.setNeighbourPropertyDraw($pdraw.property, $pdraw.text); $options.setNeighbourType(false); }
		|	'AFTER'  pdraw=formPropertyDraw { $options.setNeighbourPropertyDraw($pdraw.property, $pdraw.text); $options.setNeighbourType(true); }
		|	'QUICKFILTER' pdraw=formPropertyDraw { $options.setQuickFilterPropertyDraw($pdraw.property); }
		|	'ON' et=formEventType prop=formActionPropertyObject { $options.addEditAction($et.type, $prop.action); }
		|	'ON' 'SHORTCUT' (c=localizedStringLiteral)? prop=formActionPropertyObject { $options.addContextMenuEditAction($c.val, $prop.action); }
		|	'EVENTID' id=stringLiteral { $options.setEventId($id.val); }
		)*
	;

formPropertyDraw returns [PropertyDrawEntity property]
	:	id=ID              	{ if (inPropParseState()) $property = $formStatement::form.getPropertyDraw($id.text, self.getVersion()); }
	|	prop=mappedPropertyDraw { if (inPropParseState()) $property = $formStatement::form.getPropertyDraw($prop.name, $prop.mapping, self.getVersion()); }
	;

formMappedPropertiesList returns [List<String> aliases, List<LocalizedString> captions, List<AbstractPropertyUsage> properties, List<List<String>> mapping, List<FormPropertyOptions> options, List<DebugInfo.DebugPoint> points]
@init {
	$aliases = new ArrayList<String>();
	$captions = new ArrayList<LocalizedString>();
	$properties = new ArrayList<AbstractPropertyUsage>();
	$mapping = new ArrayList<List<String>>();
	$options = new ArrayList<FormPropertyOptions>();
	$points = new ArrayList<DebugInfo.DebugPoint>(); 
	String alias = null;
	LocalizedString caption = null;
	LP lp = null;
    List<ResolveClassSet> signature = null;
	List<String> mapping = null;
}
	:	
		{ alias = null; caption = null; $points.add(getCurrentDebugPoint()); }
		(		
				(id=simpleNameOrWithCaption '=' { alias = $id.name; caption = $id.caption; })? mappedProp=formMappedProperty 
				{
        			$properties.add($mappedProp.propUsage);
		        	$mapping.add($mappedProp.mapping);
				} 
			| 	
			    (
			    (id=simpleNameOrWithCaption { alias = $id.name; caption = $id.caption; })?
			    '='
			    (       expr=formExprDeclaration { lp = $expr.property; mapping = $expr.mapping; signature = $expr.signature; }
			        |   action=formActionDeclaration { lp = $action.property; mapping = $action.mapping; signature = $action.signature; })
                )
                {
                    LPUsage lpUsage = null;
                    if(inPropParseState()) {
                        lpUsage = new LPUsage(lp, signature);
                        assert self.checkPropertyIsNew(lpUsage);
                    }
                    $properties.add(lpUsage);
                    $mapping.add(mapping);
                } 
		)
		opts=formPropertyOptionsList
		{
			$aliases.add(alias);
			$captions.add(caption);
			$options.add($opts.options);
		}
		(','
            { alias = null; caption = null; $points.add(getCurrentDebugPoint()); }
            (		
                    (id=simpleNameOrWithCaption '=' { alias = $id.name; caption = $id.caption; })? mappedProp=formMappedProperty 
                    {
                        $properties.add($mappedProp.propUsage);
                        $mapping.add($mappedProp.mapping);
                    } 
                | 	
                    (
                    (id=simpleNameOrWithCaption { alias = $id.name; caption = $id.caption; })?
                    '='
                    (       expr=formExprDeclaration { lp = $expr.property; mapping = $expr.mapping; signature = $expr.signature; }
                        |   action=formActionDeclaration { lp = $action.property; mapping = $action.mapping; signature = $action.signature; })
                    )
                    {
                        LPUsage lpUsage = null;
                        if(inPropParseState()) {
                            lpUsage = new LPUsage(lp, signature);
                            assert self.checkPropertyIsNew(lpUsage);
                        }
                        $properties.add(lpUsage);
                        $mapping.add(mapping);
                    }
            )
            opts=formPropertyOptionsList
            {
                $aliases.add(alias);
    			$captions.add(caption);
                $options.add($opts.options);
            }
		)*
	;

formCalcPropertyObject returns [CalcPropertyObjectEntity property = null]
@init {
    AbstractPropertyUsage propUsage = null;
    List<String> mapping = null;
}
	:	(
	        mProperty=formMappedProperty { propUsage = $mProperty.propUsage; mapping = $mProperty.mapping; }
	    |
	        expr=formExprDeclaration { propUsage = new LPUsage($expr.property); mapping = $expr.mapping; }
        )
		{
			if (inPropParseState()) {
				$property = $formStatement::form.addCalcPropertyObject(propUsage, mapping);
			}
		}
	;

formActionPropertyObject returns [ActionPropertyObjectEntity action = null]
@init {
    AbstractPropertyUsage propUsage = null;
    List<String> mapping = null;
}
	:	(
	            mProperty=formMappedProperty { propUsage = $mProperty.propUsage; mapping = $mProperty.mapping; }
	        |
	            mAction=formActionDeclaration { propUsage = new LPUsage($mAction.property); mapping = $mAction.mapping; }
        )
		{
			if (inPropParseState()) {
				$action = $formStatement::form.addActionPropertyObject(propUsage, mapping);
			}
		}
	;

formGroupObjectEntity returns [GroupObjectEntity groupObject]
	:	id = ID { 
			if (inPropParseState()) {
				$groupObject = $formStatement::form.getGroupObjectEntity($ID.text, self.getVersion());
			} 
		}
	;

formMappedProperty returns [PropertyUsage propUsage, List<String> mapping]
	:	pu=formPropertyUsage { $propUsage = $pu.propUsage; }
		'('
			objects=idList { $mapping = $objects.ids; }
		')'
	;

formPropertySelector[FormEntity form] returns [PropertyDrawEntity propertyDraw = null]
	:	pname=ID
		{
			if (inPropParseState()) {
				$propertyDraw = form == null ? null : ScriptingFormEntity.getPropertyDraw(self, form, $pname.text, self.getVersion());
			}
		}
	|	mappedProp=mappedPropertyDraw	
		{
			if (inPropParseState()) {
				$propertyDraw = ScriptingFormEntity.getPropertyDraw(self, form, $mappedProp.name, $mappedProp.mapping, self.getVersion());
			}
		}
	;

mappedPropertyDraw returns [String name, List<String> mapping]
	:	pDrawName=ID { $name = $pDrawName.text; }
		'('
		list=idList { $mapping = $list.ids; }
		')'
	;

formPropertyUList returns [List<String> aliases, List<LocalizedString> captions, List<PropertyUsage> properties, List<FormPropertyOptions> options, List<DebugInfo.DebugPoint> points]
@init {
	$aliases = new ArrayList<String>();
	$captions = new ArrayList<LocalizedString>();
	$properties = new ArrayList<PropertyUsage>();
	$options = new ArrayList<FormPropertyOptions>();
	$points = new ArrayList<DebugInfo.DebugPoint>();
	String alias = null;
	LocalizedString caption = null;
}
	:	{ alias = null; caption = null; $points.add(getCurrentDebugPoint()); }
		(id=simpleNameOrWithCaption '=' { alias = $id.name; caption = $id.caption; })?
		pu=formPropertyUsage opts=formPropertyOptionsList
		{
			$aliases.add(alias);
			$captions.add(caption);
			$properties.add($pu.propUsage);
			$options.add($opts.options);
		}
		(','
			{ alias = null; caption = null; $points.add(getCurrentDebugPoint()); }
			(id=simpleNameOrWithCaption '=' { alias = $id.name; caption = $id.caption; })?
			pu=formPropertyUsage opts=formPropertyOptionsList
			{
				$aliases.add(alias);
				$captions.add(caption);
				$properties.add($pu.propUsage);
				$options.add($opts.options);
			}
		)*
	;


formPropertyUsage returns [PropertyUsage propUsage]
@init {
   String systemName = null;
   List<String> signature = null;
}
   :   pu=propertyUsage   { $propUsage = $pu.propUsage; }
       |
       (
          (
             (
                cid='NEW'      { systemName = $cid.text; }
             |  cid='NEWEDIT'  { systemName = $cid.text; }
             |  cid='EDIT'    { systemName = $cid.text; }
             )
             ( '[' clId=compoundID ']'  { signature = Collections.singletonList($clId.sid); } )?
          )
          |  cid='VALUE'    { systemName = $cid.text; }
          |  cid='DELETE'      { systemName = $cid.text; }
       ) { $propUsage = new PropertyUsage(systemName, signature); }
   ;


formFiltersList
@init {
	List<LP> properties = new ArrayList<LP>();
	List<List<String>> propertyMappings = new ArrayList<List<String>>();
}
@after {
	if (inPropParseState()) {
		$formStatement::form.addScriptedFilters(properties, propertyMappings, self.getVersion());
	}
}
	:	'FILTERS'
		decl=formExprDeclaration { properties.add($decl.property); propertyMappings.add($decl.mapping);}
	    (',' decl=formExprDeclaration { properties.add($decl.property); propertyMappings.add($decl.mapping);})*
	;

formHintsList
@init {
	boolean hintNoUpdate = true;
}
@after {
	if (inPropParseState()) {
		$formStatement::form.addScriptedHints(hintNoUpdate, $list.propUsages, self.getVersion());
	}
}
	:	(('HINTNOUPDATE') | ('HINTTABLE' { hintNoUpdate = false; })) 'LIST'
		list=nonEmptyPropertyUsageList	
	;

formEventsList
@init {
	List<ActionPropertyObjectEntity> actions = new ArrayList<ActionPropertyObjectEntity>();
	List<Object> types = new ArrayList<Object>();
}
@after {
	if (inPropParseState()) {
		$formStatement::form.addScriptedFormEvents(actions, types, self.getVersion());
	}
}
	:	'EVENTS'
		decl=formEventDeclaration { actions.add($decl.action); types.add($decl.type); }
		(',' decl=formEventDeclaration { actions.add($decl.action); types.add($decl.type); })*
	;


formEventDeclaration returns [ActionPropertyObjectEntity action, Object type]
@init {
    Boolean before = null;
}
	:	'ON'
		(	'OK' ('BEFORE' { before = true; } | 'AFTER' { before = false; })? { $type = before == null ? FormEventType.OK : (before ? FormEventType.BEFOREOK : FormEventType.AFTEROK); }
		|	'APPLY' ('BEFORE' { before = true; } | 'AFTER' { before = false; })? { $type = before == null ? FormEventType.APPLY : (before ? FormEventType.BEFOREAPPLY : FormEventType.AFTERAPPLY); }
		|	'CLOSE'	 { $type = FormEventType.CLOSE; }
		|	'INIT'	 { $type = FormEventType.INIT; }
		|	'CANCEL' { $type = FormEventType.CANCEL; }
		|	'DROP'	 { $type = FormEventType.DROP; }
		|	'QUERYOK'	 { $type = FormEventType.QUERYOK; }
		|	'QUERYCLOSE'	 { $type = FormEventType.QUERYCLOSE; }
		| 	'CHANGE' objectId=ID { $type = $objectId.text; }
		)
		faprop=formActionPropertyObject { $action = $faprop.action; }
	;


filterGroupDeclaration
@init {
	String filterGroupSID = null;
	List<RegularFilterInfo> filters = new ArrayList<RegularFilterInfo>();
}
@after {
	if (inPropParseState()) {
		$formStatement::form.addScriptedRegularFilterGroup(filterGroupSID, filters, self.getVersion());
	}
}
	:	'FILTERGROUP' sid=ID { filterGroupSID = $sid.text; }
		( rf=formRegularFilterDeclaration { filters.add($rf.filter); } )*
	;

extendFilterGroupDeclaration
@init {
	String filterGroupSID = null;
	List<RegularFilterInfo> filters = new ArrayList<RegularFilterInfo>();
}
@after {
	if (inPropParseState()) {
		$formStatement::form.extendScriptedRegularFilterGroup(filterGroupSID, filters, self.getVersion());
	}
}
	:	'EXTEND'	
		'FILTERGROUP' sid=ID { filterGroupSID = $sid.text; }
		( rf=formRegularFilterDeclaration { filters.add($rf.filter); } )+
	;
	
formRegularFilterDeclaration returns [RegularFilterInfo filter]
@init {
	String key = null;
}
    :   'FILTER' caption=localizedStringLiteral fd=formExprDeclaration (keystroke=stringLiteral {key = $keystroke.val;})? setDefault=filterSetDefault
        {
            $filter = new RegularFilterInfo($caption.val, key, $fd.property, $fd.mapping, $setDefault.isDefault);
        }
    ;
	
formExprDeclaration returns [LP property, List<String> mapping, List<ResolveClassSet> signature]
@init {
	List<TypedParameter> context = new ArrayList<TypedParameter>();
	if (inPropParseState()) {
		context = $formStatement::form.getTypedObjectsNames(self.getVersion());
	}
}
@after {
	if (inPropParseState()) {
		$mapping = self.getUsedNames(context, $expr.property.usedParams);
		$signature = self.getUsedClasses(context, $expr.property.usedParams);
	}	
}
	:	expr=propertyExpression[context, false] { if (inPropParseState()) { self.checkNecessaryProperty($expr.property); $property = $expr.property.property; } }
	;

formActionDeclaration returns [LP property, List<String> mapping, List<ResolveClassSet> signature]
@init {
	List<TypedParameter> context = new ArrayList<TypedParameter>();
	if (inPropParseState()) {
		context = $formStatement::form.getTypedObjectsNames(self.getVersion());
	}
}
@after {
	if (inPropParseState()) {
		$mapping = self.getUsedNames(context, $action.property.usedParams);
		$signature = self.getUsedClasses(context, $action.property.usedParams);
	}
}
	:	action=listTopContextDependentActionDefinitionBody[context, false, false] { if (inPropParseState()) { $property = $action.property.property; } }
	;
	
filterSetDefault returns [boolean isDefault = false]
	:	('DEFAULT' { $isDefault = true; })?
	;

formOrderByList
@init {
	boolean ascending = true;
	List<PropertyDrawEntity> properties = new ArrayList<PropertyDrawEntity>();
	List<Boolean> orders = new ArrayList<Boolean>();
}
@after {
	if (inPropParseState()) {
		$formStatement::form.addScriptedDefaultOrder(properties, orders, self.getVersion());
	}
}
	:	'ORDER' 'BY' orderedProp=formPropertyDrawWithOrder { properties.add($orderedProp.property); orders.add($orderedProp.order); }
		(',' orderedProp=formPropertyDrawWithOrder { properties.add($orderedProp.property); orders.add($orderedProp.order); } )*
	;
	
formPropertyDrawWithOrder returns [PropertyDrawEntity property, boolean order = true]
	:	pDraw=formPropertyDraw { $property = $pDraw.property; } ('DESC' { $order = false; })?
	;

////////////////////////////////////////////////////////////////////////////////
//////////////////////////////// PROPERTY STATEMENT ////////////////////////////
////////////////////////////////////////////////////////////////////////////////

propertyStatement
@init {
	LP property = null;
	List<TypedParameter> context = new ArrayList<TypedParameter>();
	List<ResolveClassSet> signature = null; 
	boolean dynamic = true;
	DebugInfo.DebugPoint point = getCurrentDebugPoint();

	String propertyName = null;
	LocalizedString caption = null;
	PropertySettings ps = null;
}
@after {
	if (inPropParseState()) {
	    if (property != null) // not native
		    self.setPropertyScriptInfo(property, $text, point);
	}
}
	:	declaration=propertyDeclaration { if ($declaration.params != null) { context = $declaration.params; dynamic = false; } }
	    {
	        propertyName = $declaration.name;
	        caption = $declaration.caption;
	    }
		'='
		(	( 
		        pdef=propertyDefinition[context, dynamic] { property = $pdef.property; signature = $pdef.signature; }
            |	ciADB=contextIndependentActionDB { if(inPropParseState()) { property = $ciADB.property; signature = $ciADB.signature; } }		
	        )
            ((opt=propertyOptions[property, propertyName, caption, context, signature] { ps = $opt.ps; } ) | ';')
        |   aDB=listTopContextDependentActionDefinitionBody[context, dynamic, true] { if (inPropParseState()) { property = $aDB.property.property; signature = self.getClassesFromTypedParams(context); }}
            (opt=propertyOptions[property, propertyName, caption, context, signature]  { ps = $opt.ps; } )?
        )
        {
            if (inPropParseState() && property != null) { // not native
                if(ps == null)
                    ps = new PropertySettings();
                property = self.addSettingsToProperty(property, propertyName, caption, context, signature, ps.groupName, ps.isPersistent, ps.isComplex, ps.noHint, ps.table, ps.notNull, ps.notNullResolve, ps.notNullEvent, ps.annotation, ps.isLoggable);
            }
        }
	;

propertyDefinition[List<TypedParameter> context, boolean dynamic] returns [LP property, List<ResolveClassSet> signature]
	:	ciPD=contextIndependentPD[false] { $property = $ciPD.property; $signature = $ciPD.signature; }
	|	expr=propertyExpression[context, dynamic] { if (inPropParseState()) { self.checkNecessaryProperty($expr.property); $signature = self.getClassesFromTypedParams(context); $property = $expr.property.property; } }
	|	'NATIVE' classId '(' clist=classIdList ')' { if (inPropParseState()) { $signature = self.createClassSetsFromClassNames($clist.ids); }}
	;


propertyDeclaration returns [String name, LocalizedString caption, List<TypedParameter> params]
	:	propNameCaption=simpleNameWithCaption { $name = $propNameCaption.name; $caption = $propNameCaption.caption; }
		('(' paramList=typedParameterList ')' { $params = $paramList.params; })? 
	;


propertyExpression[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property]
@init {
	DebugInfo.DebugPoint point = getCurrentDebugPoint();
}
@after{
    if (inPropParseState()) {
        self.propertyDefinitionCreated($property.property, point);
    }
}
	:	pe=ifPE[context, dynamic] { $property = $pe.property; }
	;


ifPE[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property]
@init {
	List<LPWithParams> props = new ArrayList<LPWithParams>();
}
@after {
	if (inPropParseState()) {
		$property = self.addScriptedIfProp(props);
	}
} 
	:	firstExpr=orPE[context, dynamic] { props.add($firstExpr.property); }
		('IF' nextExpr=orPE[context, dynamic] { props.add($nextExpr.property); })*
	;

orPE[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property]
@init {
	List<LPWithParams> props = new ArrayList<LPWithParams>();
}
@after {
	if (inPropParseState()) {
		$property = self.addScriptedOrProp(props);
	}
} 
	:	firstExpr=xorPE[context, dynamic] { props.add($firstExpr.property); }
		('OR' nextExpr=xorPE[context, dynamic] { props.add($nextExpr.property); })*
	;

xorPE[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property]
@init {
	List<LPWithParams> props = new ArrayList<LPWithParams>();
}
@after {
	if (inPropParseState()) {
		$property = self.addScriptedXorProp(props);
	}
} 
	:	firstExpr=andPE[context, dynamic] { props.add($firstExpr.property); }
		('XOR' nextExpr=andPE[context, dynamic] { props.add($nextExpr.property); })*
	;

andPE[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property]
@init {
	List<LPWithParams> props = new ArrayList<LPWithParams>();
}
@after {
	if (inPropParseState()) {
		$property = self.addScriptedAndProp(props);				
	}
}
	:	firstExpr=notPE[context, dynamic] { props.add($firstExpr.property); }
		('AND' nextExpr=notPE[context, dynamic] { props.add($nextExpr.property); })*
	;

notPE[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property]
@init {
	boolean notWas = false;
}
@after {
	if (inPropParseState() && notWas) { 
		$property = self.addScriptedNotProp($notExpr.property);  
	}
}
	:	'NOT' notExpr=notPE[context, dynamic] { notWas = true; } 
	|	expr=equalityPE[context, dynamic] { $property = $expr.property; } 
	;

equalityPE[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property]
@init {
	LPWithParams leftProp = null, rightProp = null;
	String op = null;
}
@after {
	if (inPropParseState() && op != null) {
		$property = self.addScriptedEqualityProp(op, leftProp, rightProp);
	} else {
		$property = leftProp;
	}
}
	:	lhs=relationalPE[context, dynamic] { leftProp = $lhs.property; }
		((operand=EQ_OPERAND { op = $operand.text; } | operand='=' { op = $operand.text; })
		rhs=relationalPE[context, dynamic] { rightProp = $rhs.property; })?
	;


relationalPE[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property]
@init {
	LPWithParams leftProp = null, rightProp = null;
	LP mainProp = null;
	String op = null;
}
@after {
	if (inPropParseState())
	{
		if (op != null) {
			$property = self.addScriptedRelationalProp(op, leftProp, rightProp);
		} else if (mainProp != null) {
			$property = leftProp;
			$property.property = self.addScriptedTypeExprProp(mainProp, leftProp);
		} else {
			$property = leftProp;
		}
	}	
}
	:	lhs=additiveORPE[context, dynamic] { leftProp = $lhs.property; }
		(
			(   operand=relOperand { op = $operand.text; }
			    rhs=additiveORPE[context, dynamic] { rightProp = $rhs.property; }
			)
		|	def=typePropertyDefinition { mainProp = $def.property; }
		)?
	;


additiveORPE[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property]
@init {
	List<LPWithParams> props = new ArrayList<LPWithParams>();
	List<String> ops = new ArrayList<String>();
}
@after {
	if (inPropParseState()) {
		$property = self.addScriptedAdditiveOrProp(ops, props);
	}
}
	:	firstExpr=additivePE[context, dynamic] { props.add($firstExpr.property); }
		(operand=ADDOR_OPERAND nextExpr=additivePE[context, dynamic] { ops.add($operand.text); props.add($nextExpr.property); })*
	;
	
	
additivePE[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property]
@init {
	List<LPWithParams> props = new ArrayList<LPWithParams>();
	List<String> ops = new ArrayList<String>();
}
@after {
	if (inPropParseState()) {
		$property = self.addScriptedAdditiveProp(ops, props);				
	}
}
	:	firstExpr=multiplicativePE[context, dynamic] { props.add($firstExpr.property); }
		( (operand=PLUS | operand=MINUS) { ops.add($operand.text); }
		nextExpr=multiplicativePE[context, dynamic] { props.add($nextExpr.property); })*
	;
		
	
multiplicativePE[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property]
@init {
	List<LPWithParams> props = new ArrayList<LPWithParams>();
	List<String> ops = new ArrayList<String>();
}
@after {
	if (inPropParseState()) {
		$property = self.addScriptedMultiplicativeProp(ops, props);				
	}
}
	:	firstExpr=unaryMinusPE[context, dynamic] { props.add($firstExpr.property); }
		(operand=multOperand { ops.add($operand.text); }
		nextExpr=unaryMinusPE[context, dynamic] { props.add($nextExpr.property); })*
	;

unaryMinusPE[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property] 
@init {
	boolean minusWas = false;
}
@after {
	if (inPropParseState() && minusWas) {
		$property = self.addScriptedUnaryMinusProp($expr.property);
	} 
}
	:	MINUS expr=unaryMinusPE[context, dynamic] { minusWas = true; } 
	|	simpleExpr=postfixUnaryPE[context, dynamic] { $property = $simpleExpr.property; }
	;

		 
postfixUnaryPE[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property] 
@init {	
	boolean hasPostfix = false;
}
@after {
	if (inPropParseState() && hasPostfix) {
		$property = self.addScriptedDCCProp($expr.property, $index.val);
	} 
}
	:	expr=simplePE[context, dynamic] { $property = $expr.property; }
		(
			'[' index=uintLiteral ']' { hasPostfix = true; }
		)?
	;		 

		 
simplePE[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property]
	:	'(' expr=propertyExpression[context, dynamic] ')' { $property = $expr.property; } 
	|	primitive=expressionPrimitive[context, dynamic] { $property = $primitive.property; } 
	;

	
expressionPrimitive[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property]
	:	param=singleParameter[context, dynamic] { $property = $param.property; }
	|	expr=expressionFriendlyPD[context, dynamic] { $property = $expr.property; }
	;

singleParameter[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property]
@init {
	String className = null;
}
@after {
	if (inPropParseState()) {
		$property = new LPWithParams(null, Collections.singletonList(self.getParamIndex(TP(className, $paramName.text), $context, $dynamic, insideRecursion)));
	}
}
	:	(clsId=classId { className = $clsId.sid; })? paramName=parameter
	;
	
expressionFriendlyPD[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property]
@after {
	if (inPropParseState()) {
		self.checkPropertyValue($property.property);
	}
}
	:	joinDef=joinPropertyDefinition[context, dynamic] { $property = $joinDef.property; } 
	|	multiDef=multiPropertyDefinition[context, dynamic] { $property = $multiDef.property; }
	|	overDef=overridePropertyDefinition[context, dynamic] { $property = $overDef.property; }
	|	ifElseDef=ifElsePropertyDefinition[context, dynamic] { $property = $ifElseDef.property; }
	|	maxDef=maxPropertyDefinition[context, dynamic] { $property = $maxDef.property; }
	|	caseDef=casePropertyDefinition[context, dynamic] { $property = $caseDef.property; }
	|	partDef=partitionPropertyDefinition[context, dynamic] { $property = $partDef.property; }
	|	recDef=recursivePropertyDefinition[context, dynamic] { $property = $recDef.property; } 
	|	structDef=structCreationPropertyDefinition[context, dynamic] { $property = $structDef.property; }
	|	concatDef=concatPropertyDefinition[context, dynamic] { $property = $concatDef.property; }
	|	castDef=castPropertyDefinition[context, dynamic] { $property = $castDef.property; }
	|	sessionDef=sessionPropertyDefinition[context, dynamic] { $property = $sessionDef.property; }
	|	signDef=signaturePropertyDefinition[context, dynamic] { $property = $signDef.property; }
	|	activeTabDef=activeTabPropertyDefinition[context, dynamic] { $property = $activeTabDef.property; }
	|	constDef=constantProperty { $property = new LPWithParams($constDef.property, new ArrayList<Integer>()); }
	;

contextIndependentPD[boolean innerPD] returns [LP property, List<ResolveClassSet> signature]
@init {
	DebugInfo.DebugPoint point = getCurrentDebugPoint();
}
@after{
	if (inPropParseState()) {
		self.propertyDefinitionCreated($property, point);
	}
}
	: 	dataDef=dataPropertyDefinition[innerPD] { $property = $dataDef.property; $signature = $dataDef.signature; }
	|	abstractDef=abstractPropertyDefinition { $property = $abstractDef.property; $signature = $abstractDef.signature; }
	|	formulaProp=formulaPropertyDefinition { $property = $formulaProp.property; $signature = $formulaProp.signature; }
	|	groupDef=groupPropertyDefinition { $property = $groupDef.property; $signature = $groupDef.signature; }
	|	filterProp=filterPropertyDefinition { $property = $filterProp.property; $signature = $filterProp.signature; }
	|	reflectionDef=reflectionPropertyDefinition { $property = $reflectionDef.property; $signature = $reflectionDef.signature;  }
	;

joinPropertyDefinition[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property]
@init {
	boolean isInline = false;
}
@after {
	if (inPropParseState()) {
		if (isInline) {
			$property = self.addScriptedJProp(true, $iProp.property, $exprList.props);
		} else {
			$property = self.addScriptedJProp(true, $uProp.propUsage, $exprList.props, context);	
		}
	}
}
	:	('JOIN')? 
		(	uProp=propertyUsage
		|	iProp=inlineProperty { isInline = true; }
		)
		'('
		exprList=propertyExpressionList[context, dynamic]
		')'
	;


groupPropertyDefinition returns [LP property, List<ResolveClassSet> signature]
@init {
	List<LPWithParams> orderProps = new ArrayList<LPWithParams>();
	List<LPWithParams> groupProps = new ArrayList<LPWithParams>();
	List<TypedParameter> groupContext = new ArrayList<TypedParameter>();
	boolean ascending = true;
}
@after {
	if (inPropParseState()) {
		$signature = self.getSignatureForGProp(groupProps, groupContext);
		$property = self.addScriptedGProp($type.type, $mainList.props, groupProps, orderProps, ascending, $whereExpr.property, groupContext);
	}
}
	:	'GROUP'
		type=groupingType
		mainList=nonEmptyPropertyExpressionList[groupContext, true]
		('BY' exprList=nonEmptyPropertyExpressionList[groupContext, true] { groupProps.addAll($exprList.props); })?
		('ORDER' ('DESC' { ascending = false; } )?
		orderList=nonEmptyPropertyExpressionList[groupContext, true] { orderProps.addAll($orderList.props); })?
		('WHERE' whereExpr=propertyExpression[groupContext, false])?
	;


groupingType returns [GroupingType type]
	:	'SUM' 	{ $type = GroupingType.SUM; }
	|	'MAX' 	{ $type = GroupingType.MAX; }
	|	'MIN' 	{ $type = GroupingType.MIN; }
	|	'CONCAT' { $type = GroupingType.CONCAT; }
	|	'AGGR' { $type = GroupingType.AGGR; }
	|	'NAGGR' { $type = GroupingType.NAGGR; }
	|	'EQUAL'	{ $type = GroupingType.EQUAL; }	
	|	'LAST'	{ $type = GroupingType.LAST; }
	;


partitionPropertyDefinition[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property]
@init {
	List<LPWithParams> paramProps = new ArrayList<LPWithParams>();
	PropertyUsage pUsage = null;
	PartitionType type = null;
	int groupExprCnt;
	boolean strict = false;
	int precision = 0;
	boolean ascending = true;
	boolean useLast = true;
}
@after {
	if (inPropParseState()) {
		$property = self.addScriptedPartitionProp(type, pUsage, strict, precision, ascending, useLast, groupExprCnt, paramProps, context);
	}
}
	:	'PARTITION' 
		(
			(	'SUM'	{ type = PartitionType.SUM; } 
			|	'PREV'	{ type = PartitionType.PREVIOUS; }
			)
		|	'UNGROUP'
			ungroupProp=propertyUsage { pUsage = $ungroupProp.propUsage; }
			(	'PROPORTION' { type = PartitionType.DISTR_CUM_PROPORTION; } 
				('STRICT' { strict = true; })? 
				'ROUND' '(' prec=intLiteral ')' { precision = $prec.val; }
			|	'LIMIT' { type = PartitionType.DISTR_RESTRICT; } 
				('STRICT' { strict = true; })? 
			)
		)
		expr=propertyExpression[context, dynamic] { paramProps.add($expr.property); }
		(	'BY'
			exprList=nonEmptyPropertyExpressionList[context, dynamic] { paramProps.addAll($exprList.props); }
		)?
		{ groupExprCnt = paramProps.size() - 1; }
		(	'ORDER' ('DESC' { ascending = false; } )?				
			orderList=nonEmptyPropertyExpressionList[context, dynamic] { paramProps.addAll($orderList.props); }
		)? 
		('WINDOW' 'EXCEPTLAST' { useLast = false; })?
	;


dataPropertyDefinition[boolean innerPD] returns [LP property, List<ResolveClassSet> signature]
@init {
	boolean localProp = false;
	LocalNestedType nestedType = null;
}
@after {
	if (inPropParseState()) {
		$signature = self.createClassSetsFromClassNames($paramClassNames.ids); 
		$property = self.addScriptedDProp($returnClass.sid, $paramClassNames.ids, localProp, innerPD, false, nestedType);
	}
}
	:	'DATA'
		('LOCAL' nlm=nestedLocalModifier { localProp = true; nestedType = $nlm.nestedType; })?
		returnClass=classId
		'('
			paramClassNames=classIdList
		')'
	;

nestedLocalModifier returns[LocalNestedType nestedType = null]
	:	('NESTED' { $nestedType = LocalNestedType.ALL; }
	        (   'MANAGESESSION' { $nestedType = LocalNestedType.MANAGESESSION; }
	        |   'NOMANAGESESSION' { $nestedType = LocalNestedType.NOMANAGESESSION; }
	        )?
        )?
	;

abstractPropertyDefinition returns [LP property, List<ResolveClassSet> signature]
@init {
	boolean isExclusive = true;
	boolean isLast = false;
	boolean isChecked = false;
	CaseUnionProperty.Type type = CaseUnionProperty.Type.MULTI;	
}
@after {
	if (inPropParseState()) {
		$signature = self.createClassSetsFromClassNames($paramClassNames.ids); 
		$property = self.addScriptedAbstractProp(type, $returnClass.sid, $paramClassNames.ids, isExclusive, isChecked, isLast);
	}
}
	:	'ABSTRACT'
		(
			(	'CASE' { type = CaseUnionProperty.Type.CASE; isExclusive = false; }
			|	'MULTI'	{ type = CaseUnionProperty.Type.MULTI; isExclusive = true; } 
			|   'VALUE' { type = CaseUnionProperty.Type.VALUE; isExclusive = false; } 
			)
			(opt=abstractExclusiveOverrideOption { isExclusive = $opt.isExclusive; if($opt.isLast != null) isLast = $opt.isLast;})?
		)?
		('CHECKED' { isChecked = true; })?
		returnClass=classId
		'('
			paramClassNames=classIdList
		')'
	;

abstractActionDefinition returns [LP property, List<ResolveClassSet> signature]
@init {
	boolean isExclusive = true;
	boolean isLast = true;
	boolean isChecked = false;
	ListCaseActionProperty.AbstractType type = ListCaseActionProperty.AbstractType.MULTI;
}
@after {
	if (inPropParseState()) {
		$signature = self.createClassSetsFromClassNames($paramClassNames.ids); 
		$property = self.addScriptedAbstractActionProp(type, $paramClassNames.ids, isExclusive, isChecked, isLast);
	}
}
	:	'ABSTRACT'
		(
			(('CASE' { type = ListCaseActionProperty.AbstractType.CASE; isExclusive = false; isLast = false; }
			|	'MULTI'	{ type = ListCaseActionProperty.AbstractType.MULTI; isExclusive = true; isLast = false; }) (opt=abstractExclusiveOverrideOption { isExclusive = $opt.isExclusive; if($opt.isLast!=null) isLast = $opt.isLast;})?)
		|	('LIST' { type = ListCaseActionProperty.AbstractType.LIST; } (acopt=abstractCaseAddOption { isLast = $acopt.isLast; } )?)
		)?
		('CHECKED' { isChecked = true; })?
		'(' 
			paramClassNames=classIdList
		')'	
	;
	
overridePropertyDefinition[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property]
@init {
	boolean isExclusive = false;
}
@after {
	if (inPropParseState()) {
		$property = self.addScriptedOverrideProp($exprList.props, isExclusive);
	}
}
	:	(('OVERRIDE') | ('EXCLUSIVE' { isExclusive = true; })) 
		exprList=nonEmptyPropertyExpressionList[context, dynamic] 
	;


ifElsePropertyDefinition[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property]
@init {
	LPWithParams elseProp = null;
}
@after {
	if (inPropParseState()) {
		$property = self.addScriptedIfElseUProp($ifExpr.property, $thenExpr.property, elseProp);
	}
}
	:	'IF' ifExpr=propertyExpression[context, dynamic]
		'THEN' thenExpr=propertyExpression[context, dynamic]
		('ELSE' elseExpr=propertyExpression[context, dynamic] { elseProp = $elseExpr.property; })?
	;


maxPropertyDefinition[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property]
@init {
	boolean isMin = true;
}
@after {
	if (inPropParseState()) {
		$property = self.addScriptedMaxProp($exprList.props, isMin);
	}
}
	:	(('MAX') { isMin = false; } | ('MIN'))
		exprList=nonEmptyPropertyExpressionList[context, dynamic]	
	;


casePropertyDefinition[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property]
@init {
	List<LPWithParams> whenProps = new ArrayList<LPWithParams>();
	List<LPWithParams> thenProps = new ArrayList<LPWithParams>();
	LPWithParams elseProp = null;
	boolean isExclusive = false;
}
@after {
	if (inPropParseState()) {
		$property = self.addScriptedCaseUProp(whenProps, thenProps, elseProp, isExclusive);
	}
}
	:	'CASE' (opt=exclusiveOverrideOption { isExclusive = $opt.isExclusive; })?
			( branch=caseBranchBody[context, dynamic] { whenProps.add($branch.whenProperty); thenProps.add($branch.thenProperty); } )+
			('ELSE' elseExpr=propertyExpression[context, dynamic] { elseProp = $elseExpr.property; })?
	;
	
	
caseBranchBody[List<TypedParameter> context, boolean dynamic] returns [LPWithParams whenProperty, LPWithParams thenProperty]
	:	'WHEN' whenExpr=propertyExpression[context, dynamic] { $whenProperty = $whenExpr.property; }
		'THEN' thenExpr=propertyExpression[context, dynamic] { $thenProperty = $thenExpr.property; }
	;

multiPropertyDefinition[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property]
@init {
	boolean isExclusive = true;
}
@after {
	if (inPropParseState()) {
		$property = self.addScriptedMultiProp($exprList.props, isExclusive);
	}
}
	:	'MULTI' 
		exprList=nonEmptyPropertyExpressionList[context, dynamic] 
		(opt=exclusiveOverrideOption { isExclusive = $opt.isExclusive; })?
	;

recursivePropertyDefinition[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property]
@init {
	Cycle cycleType = Cycle.NO;
	List<TypedParameter> recursiveContext = null;
	if (inPropParseState() && insideRecursion) {
		self.getErrLog().emitNestedRecursionError(self.getParser());
	}
}
@after {
	if (inPropParseState()) {
		$property = self.addScriptedRProp(recursiveContext, $zeroStep.property, $nextStep.property, cycleType);			
	}
	insideRecursion = false;
}
	:	'RECURSION'
		zeroStep=propertyExpression[context, dynamic]
		'STEP'
		{ 
			insideRecursion = true; 
		  	recursiveContext = new ArrayList<TypedParameter>(context);
		}
		nextStep=propertyExpression[recursiveContext, dynamic]
		('CYCLES' 
			(	'YES' { cycleType = Cycle.YES; }
			|	'NO' { cycleType = Cycle.NO; } 
			|	'IMPOSSIBLE' { cycleType = Cycle.IMPOSSIBLE; }
			)
		)?
	;

structCreationPropertyDefinition[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property] 
@after {
	if (inPropParseState()) {
		$property = self.addScriptedCCProp($list.props);		
	}
}
	:	'STRUCT'
		'('
		list=nonEmptyPropertyExpressionList[context, dynamic]
		')' 
	;

castPropertyDefinition[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property]
@after {
	if (inPropParseState()) {
		$property = self.addScriptedCastProp($ptype.text, $expr.property);
	}
}
	:   ptype=PRIMITIVE_TYPE '(' expr=propertyExpression[context, dynamic] ')'
	;

concatPropertyDefinition[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property]
@after {
	if (inPropParseState()) {
		$property = self.addScriptedConcatProp($separator.val, $list.props);
	}
}
	:   'CONCAT' separator=stringLiteral ',' list=nonEmptyPropertyExpressionList[context, dynamic]
	;

sessionPropertyDefinition[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property]
@init {
	IncrementType type = null; 
}
@after {
	if (inPropParseState()) {
		$property = self.addScriptedSessionProp(type, $expr.property);
	}
}
	:	(	'PREV' { type = null; } 
		| 	'CHANGED' { type = IncrementType.CHANGED; }
		| 	'SET' { type = IncrementType.SET; }
		| 	'DROPPED' { type = IncrementType.DROP; }
		| 	'SETCHANGED' { type = IncrementType.SETCHANGED; }
		|	'DROPCHANGED' { type = IncrementType.DROPCHANGED; }
		| 	'DROPSET' { type = IncrementType.DROPSET; }
		)
		'('
		expr=propertyExpression[context, dynamic] 
		')'
	;

signaturePropertyDefinition[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property] 	
@after {
	if (inPropParseState()) {
		$property = self.addScriptedSignatureProp($expr.property);
	}
} 
	: 	'CLASS' '(' expr=propertyExpression[context, dynamic] ')'
	;

activeTabPropertyDefinition[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property]
@after {
	if (inPropParseState()) {
		$property = self.addScriptedActiveTabProp($fc.form, $fc.component);
	}
}
	: 	'ACTIVE' 'TAB' fc = formComponentID
	;

formulaPropertyDefinition returns [LP property, List<ResolveClassSet> signature]
@init {
	String className = null;
	boolean hasNotNullCondition = false;
}
@after {
	if (inPropParseState()) {
		$property = self.addScriptedSFProp(className, $synt.types, $synt.strings, hasNotNullCondition);
		$signature = Collections.<ResolveClassSet>nCopies($property.listInterfaces.size(), null);
	}
}
	:	'FORMULA'
		('NULL' { hasNotNullCondition = true; })?
		(clsName=classId { className = $clsName.sid; })?
		synt=formulaPropertySyntaxList
	;

formulaPropertySyntaxList returns [List<SQLSyntaxType> types = new ArrayList<SQLSyntaxType>(), List<String> strings = new ArrayList<String>()]
	:	firstType=formulaPropertySyntaxType firstText=stringLiteral { $types.add($firstType.type); $strings.add($firstText.val); }
		(',' nextType=formulaPropertySyntaxType nextText=stringLiteral { $types.add($nextType.type); $strings.add($nextText.val); })*
	;

formulaPropertySyntaxType returns [SQLSyntaxType type = null]
	:	('PG' { $type = SQLSyntaxType.POSTGRES; } | 'MS' { $type = SQLSyntaxType.MSSQL; })? 
	;

filterPropertyDefinition returns [LP property, List<ResolveClassSet> signature]
@init {
	String className = null;
	GroupObjectProp prop = null;
}
@after {
	if (inPropParseState()) {
		$signature = new ArrayList<ResolveClassSet>();	
		$property = self.addScriptedGroupObjectProp($gobj.sid, prop, $signature);
	}
}
	:	('FILTER' { prop = GroupObjectProp.FILTER; } | 'ORDER' { prop = GroupObjectProp.ORDER; } | 'VIEW' { prop = GroupObjectProp.VIEW; } )
		gobj=formGroupObjectID
	;
	
reflectionPropertyDefinition returns [LP property, List<ResolveClassSet> signature]
@init {
	ReflectionPropertyType type = null;
	PropertyUsage propertyUsage = null;
}
@after{
	if (inPropParseState()) {
		$signature = new ArrayList<ResolveClassSet>();	
		$property = self.addScriptedReflectionProperty(type, propertyUsage, $signature);
	}
}
	:	'REFLECTION' t=reflectionPropertyType { type = $t.type; } pu=propertyUsage { propertyUsage = $pu.propUsage; }
	;
	
reflectionPropertyType returns [ReflectionPropertyType type]
	:	'CANONICALNAME' { $type = ReflectionPropertyType.CANONICAL_NAME; }
	;

readActionDefinitionBody[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property]
@init {
	boolean delete = false;
}
@after {
	if (inPropParseState()) {
		$property = self.addScriptedReadActionProperty($expr.property, $pUsage.propUsage, $moveExpr.property, delete);
	}
}
	:	'READ' expr=propertyExpression[context, dynamic] 'TO' pUsage=propertyUsage (('MOVE' moveExpr=propertyExpression[context, dynamic]) | ('DELETE' {delete = true; }))?
	;

writeActionDefinitionBody[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property]
@after {
	if (inPropParseState()) {
		$property = self.addScriptedWriteActionProperty($expr.property, $fromExpr.property);
	}
}
	:	'WRITE' expr=propertyExpression[context, dynamic] 'FROM' fromExpr=propertyExpression[context, dynamic]
	;

importActionDefinitionBody[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property]
@init {
	ImportSourceFormat format = null;
	LPWithParams sheet = null;
	LPWithParams memo = null;
	String separator = null;
	boolean noHeader = false;
	String charset = null;
	boolean attr = false;

}
@after {
	if (inPropParseState()) {
		if($type.format == ImportSourceFormat.XLS || $type.format == ImportSourceFormat.XLSX)
			$property = self.addScriptedImportExcelActionProperty($type.format, $expr.property, $plist.ids, $plist.propUsages, $context, sheet);
		else if($type.format == ImportSourceFormat.CSV)
        	$property = self.addScriptedImportCSVActionProperty($expr.property, $plist.ids, $plist.propUsages, separator, noHeader, charset);
        else if($type.format == ImportSourceFormat.XML)
        	$property = self.addScriptedImportXMLActionProperty($expr.property, $plist.ids, $plist.propUsages, attr);
        else if($type.format == ImportSourceFormat.DBF)
            $property = self.addScriptedImportDBFActionProperty($expr.property, $whereExpr.property, memo, $plist.ids, $plist.propUsages, charset);
		else
			$property = self.addScriptedImportActionProperty($type.format, $expr.property, $plist.ids, $plist.propUsages);
	}
} 
	:	'IMPORT' 
		type = importSourceFormat [context, dynamic] { format = $type.format; sheet = $type.sheet; memo = $type.memo; separator = $type.separator; noHeader = $type.noHeader; attr = $type.attr; charset = $type.charset; }
		'TO' plist=nonEmptyPropertyUsageListWithIds 
		'FROM' expr=propertyExpression[context, dynamic]
		('WHERE' whereExpr=propertyExpression[context, dynamic])?
	;

importFormActionDefinitionBody[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property]
@init {
	ImportSourceFormat format = null;
	LPWithParams sheet = null;
	LPWithParams memo = null;
	String separator = null;
	boolean noHeader = false;
	String charset = null;
	boolean attr = false;
    FormEntity form = null;
}
@after {
	if (inPropParseState()) {
	    if($type.format == ImportSourceFormat.CSV)
            $property = self.addScriptedImportFormCSVActionProperty(form, noHeader, charset, separator);
    	else if($type.format == ImportSourceFormat.DBF)
        	$property = self.addScriptedImportFormDBFActionProperty(form, charset);
	    else if($type.format == ImportSourceFormat.XML)
    		$property = self.addScriptedImportFormXMLActionProperty(form, attr);
    	else if($type.format == ImportSourceFormat.JSON)
    	    $property = self.addScriptedImportFormJSONActionProperty(form);

	}
}
	:	'IMPORT'
	    (namespace=ID '.')? formSName=ID { if (inPropParseState()) { form = self.findForm(($namespace == null ? "" : $namespace.text + ".") + $formSName.text); }}
	    type = importSourceFormat [context, dynamic] { format = $type.format; sheet = $type.sheet; memo = $type.memo; separator = $type.separator; noHeader = $type.noHeader; attr = $type.attr; charset = $type.charset; }
	;

newThreadActionDefinitionBody[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property]
@init {
	List<LPWithParams> props = new ArrayList<LPWithParams>();
	List<LP> localProps = new ArrayList<LP>();
}
@after {
	if (inPropParseState()) {
		$property = self.addScriptedNewThreadActionProperty($aDB.property, $connExpr.property, $periodExpr.property, $delayExpr.property);
	}
}
	:	'NEWTHREAD' aDB=keepContextFlowActionDefinitionBody[context, dynamic]
	    (
	    	(   'CONNECTION' connExpr=propertyExpression[context, dynamic]
		    |   'SCHEDULE' ('PERIOD' periodExpr=propertyExpression[context, dynamic])? ('DELAY' delayExpr=propertyExpression[context, dynamic])?
    	    )
    	    ';'
        )? 
	;

newExecutorActionDefinitionBody[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property]
@init {
	List<LPWithParams> props = new ArrayList<LPWithParams>();
	List<LP> localProps = new ArrayList<LP>();
}
@after {
	if (inPropParseState()) {
		$property = self.addScriptedNewExecutorActionProperty($aDB.property, $threadsExpr.property);
	}
}
	:	'NEWEXECUTOR' aDB=keepContextFlowActionDefinitionBody[context, dynamic] 'THREADS' threadsExpr=propertyExpression[context, dynamic] ';'
	;

newSessionActionDefinitionBody[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property]
@init {
	List<PropertyUsage> migrateSessionProps = Collections.emptyList();
	boolean migrateAllSessionProps = false;
	boolean isNested = false;
	boolean singleApply = false;
	boolean newSQL = false;
}
@after {
	if (inPropParseState()) {
		$property = self.addScriptedNewSessionAProp($aDB.property, migrateSessionProps, migrateAllSessionProps, isNested, singleApply, newSQL);
	}
}
	:	(	'NEWSESSION' ('NEWSQL' { newSQL = true; })? (mps=nestedPropertiesSelector { migrateAllSessionProps = $mps.all; migrateSessionProps = $mps.props; })?
		|	'NESTEDSESSION' { isNested = true; }
		)	
		('SINGLE' { singleApply = true; })? 
		aDB=keepContextFlowActionDefinitionBody[context, dynamic]
	; 

nonEmptyPropertyUsageListWithIds returns [List<String> ids, List<PropertyUsage> propUsages]
@init {
	$ids = new ArrayList<String>();
	$propUsages = new ArrayList<PropertyUsage>();
}
	:	usage = propertyUsageWithId { $ids.add($usage.id); $propUsages.add($usage.propUsage); }
		(',' usage = propertyUsageWithId { $ids.add($usage.id); $propUsages.add($usage.propUsage); })*
	;

propertyUsageWithId returns [String id = null, PropertyUsage propUsage]
	:	pu=propertyUsage { $propUsage = $pu.propUsage; }
		(	'='
			(	pid=ID { $id = $pid.text; }
			|	sLiteral=stringLiteral { $id = $sLiteral.val; } 
			)
		)? 
	;

importSourceFormat [List<TypedParameter> context, boolean dynamic] returns [ImportSourceFormat format, LPWithParams sheet, LPWithParams memo, String separator, boolean noHeader, String charset, boolean attr]
	:	'XLS' 	{ $format = ImportSourceFormat.XLS; } ('SHEET' sheetProperty = propertyExpression[context, dynamic] { $sheet = $sheetProperty.property; })?
	|	'XLSX'	{ $format = ImportSourceFormat.XLSX; } ('SHEET' sheetProperty = propertyExpression[context, dynamic] { $sheet = $sheetProperty.property; })?
	|	'DBF'	{ $format = ImportSourceFormat.DBF; } ('MEMO' memoProperty = propertyExpression[context, dynamic] {$memo = $memoProperty.property; })? ('CHARSET' charsetVal = stringLiteral { $charset = $charsetVal.val; })?
	|	'CSV'	{ $format = ImportSourceFormat.CSV; } (separatorVal = stringLiteral { $separator = $separatorVal.val; })? ('NOHEADER' { $noHeader = true; })? ('CHARSET' charsetVal = stringLiteral { $charset = $charsetVal.val; })?
	|	'XML'	{ $format = ImportSourceFormat.XML; } ('ATTR' { $attr = true; })?
	|	'JSON'	{ $format = ImportSourceFormat.JSON; }
	|	'JDBC'	{ $format = ImportSourceFormat.JDBC; }
	|	'MDB'	{ $format = ImportSourceFormat.MDB; }
	;

typePropertyDefinition returns [LP property] 
@init {
	boolean bIs = false;
}
@after {
	if (inPropParseState()) {
		$property = self.addScriptedTypeProp($clsId.sid, bIs);
	}	
}
	:	('IS' { bIs = true; } | 'AS')
		clsId=classId
	;


propertyUsage returns [PropertyUsage propUsage]  
@init {
	List<String> classList = null;
}
@after {
	$propUsage = new PropertyUsage($pname.name, classList);
}
	:	pname=propertyName ('[' cidList=signatureClassList ']' { classList = $cidList.ids; })? 
	;

inlineProperty returns [LP property]
@init {
	List<TypedParameter> newContext = new ArrayList<TypedParameter>();
	List<ResolveClassSet> signature = null;
}
@after {
	if (inPropParseState()) { // not native
		property.setExplicitClasses(signature);	
	}
}
	:	'[' '='	(	expr=propertyExpression[newContext, true] { if (inPropParseState()) { self.checkNecessaryProperty($expr.property); $property = $expr.property.property; signature = self.getClassesFromTypedParams(newContext); } }
				|	ciPD=contextIndependentPD[true] { $property = $ciPD.property; signature = $ciPD.signature; }
//                |   aDB=listTopContextDependentActionDefinitionBody[newContext, true, true] { if (inPropParseState()) { $property = $aDB.property.property; signature = self.getClassesFromTypedParams(newContext); }}
//                |	'ACTION' ciADB=contextIndependentActionDB { if (inPropParseState()) { $property = $ciADB.property; signature = $ciADB.signature; } }
				)
		']'
	;

propertyName returns [String name] 
	:	id=compoundID { $name = $id.sid; }
	;

propertyOptions[LP property, String propertyName, LocalizedString caption, List<TypedParameter> context, List<ResolveClassSet> signature] returns [PropertySettings ps = new PropertySettings()]
   //	non-LL : ( semiPropertyOption | nonSemiPropertyOption )* (semiPropertyOption ';' | nonSemiPropertyOption)
	:       recursivePropertyOptions[property, propertyName, caption, $ps, context]
	;

recursivePropertyOptions[LP property, String propertyName, LocalizedString caption, PropertySettings ps, List<TypedParameter> context]
	:       semiPropertyOption[property, propertyName, caption, ps, context] (';' | recursivePropertyOptions[property, propertyName, caption, ps, context])
	    |   nonSemiPropertyOption[property, propertyName, caption, ps, context] recursivePropertyOptions[property, propertyName, caption, ps, context]?
	;

semiPropertyOption[LP property, String propertyName, LocalizedString caption, PropertySettings ps, List<TypedParameter> context]
    :
            inSetting [ps]
        |	persistentSetting [ps]
        |	complexSetting [ps]
        |	noHintSetting [ps]
        |	tableSetting [ps]
        |	shortcutSetting [property, caption != null ? caption : LocalizedString.create(propertyName)]
        |	forceViewTypeSetting [property]
        |	fixedCharWidthSetting [property]
        |	minCharWidthSetting [property]
        |	maxCharWidthSetting [property]
        |	prefCharWidthSetting [property]
        |   defaultCompareSetting [property]
        |	imageSetting [property]
        |	editKeySetting [property]
        |	autosetSetting [property]
        |	confirmSetting [property]
        |	regexpSetting [property]
        |	loggableSetting [ps]
        |	echoSymbolsSetting [property]
        |	indexSetting [property]
        |	aggPropSetting [property]
        |	setNotNullSetting [ps]
        |	asonEditActionSetting [property]
        |	eventIdSetting [property]
        |   '@@' ann = ID { ps.annotation = $ann.text; }
    ;

nonSemiPropertyOption[LP property, String propertyName, LocalizedString caption, PropertySettings ps, List<TypedParameter> context]
    :
        onEditEventSetting [property, context]
    ;

inSetting [PropertySettings ps]
	:	'IN' name=compoundID { ps.groupName = $name.sid; }
	;
persistentSetting [PropertySettings ps]
	:	'MATERIALIZED' { ps.isPersistent = true; }
	;
complexSetting [PropertySettings ps]
	:	'COMPLEX' { ps.isComplex = true; }
	;
noHintSetting [PropertySettings ps]
	:	'NOHINT' { ps.noHint = true; }
	;
tableSetting [PropertySettings ps]
	:	'TABLE' tbl = compoundID { ps.table = $tbl.sid; }
	;
loggableSetting [PropertySettings ps]
	:	'LOGGABLE'  { ps.isLoggable = true; }
	;
setNotNullSetting [PropertySettings ps]
    :   s=notNullSetting {
                        ps.notNull = new BooleanDebug($s.debugPoint);
                        ps.notNullResolve = $s.toResolve;
                        ps.notNullEvent = $s.event;
        			}
    ;
annotationSetting [PropertySettings ps]
	:
	    '@@' ann = ID { ps.annotation = $ann.text; }
	;

notNullSetting returns [DebugInfo.DebugPoint debugPoint, BooleanDebug toResolve = null, Event event]
@init {
    $debugPoint = getEventDebugPoint();
}
	:	'NONULL'
	    (dt = notNullDeleteSetting { $toResolve = new BooleanDebug($dt.debugPoint); })?
	    et=baseEvent { $event = $et.event; }
	;


shortcutSetting [LP property, LocalizedString caption]
@after {
	if (inPropParseState()) {
		self.addToContextMenuFor(property, caption, $usage.propUsage);
	}
}
	:	'SHORTCUT' usage = propertyUsage 
	;

asonEditActionSetting [LP property]
@init {
	String editActionSID = null;
}
@after {
	if (inPropParseState()) {
		self.setAsEditActionFor(property, $et.type, $usage.propUsage);
	}
}
	:	'ASON' et=formEventType usage=propertyUsage 
	;

forceViewTypeSetting [LP property]
@after {
	if (inPropParseState()) {
		self.setForceViewType(property, $viewType.type);
	}
}
	:	viewType=classViewType
	;

fixedCharWidthSetting [LP property]
@after {
	if (inPropParseState()) {
		self.setFixedCharWidth(property, $width.val);
	}
}
	:	'FIXEDCHARWIDTH' width = intLiteral
	;

minCharWidthSetting [LP property]
@after {
	if (inPropParseState()) {
		self.setMinCharWidth(property, $width.val);
	}
}
	:	'MINCHARWIDTH' width = intLiteral
	;

maxCharWidthSetting [LP property]
@after {
	if (inPropParseState()) {
		self.setMaxCharWidth(property, $width.val);
	}
}
	:	'MAXCHARWIDTH' width = intLiteral
	;

prefCharWidthSetting [LP property]
@after {
	if (inPropParseState()) {
		self.setPrefCharWidth(property, $width.val);
	}
}
	:	'PREFCHARWIDTH' width = intLiteral
	;

imageSetting [LP property]
@after {
	if (inPropParseState()) {
		self.setImage(property, $path.val);
	}
}
	:	'IMAGE' path = stringLiteral
	;

defaultCompareSetting [LP property]
@after {
	if (inPropParseState()) {
		self.setDefaultCompare(property, $defaultCompare.val);
	}
}
	:	'DEFAULTCOMPARE' defaultCompare = stringLiteral
	;


editKeySetting [LP property]
@init {
	Boolean show = null;
}
@after {
	if (inPropParseState()) {
		self.setEditKey(property, $key.val, show);
	}
}
	:	'EDITKEY' key = stringLiteral
		(	('SHOW' { show = true; })
		|	('HIDE' { show = false; })
		)?
	;

autosetSetting [LP property]
@init {
	boolean autoset = false;
}
@after {
	if (inPropParseState()) {
		self.setAutoset(property, autoset);
	}
}
	:	'AUTOSET' { autoset = true; }
	;

confirmSetting [LP property]
@init {
	boolean askConfirm = false;
}
@after {
	if (inPropParseState()) {
		self.setAskConfirm(property, askConfirm);
	}
}
	:	'CONFIRM' { askConfirm = true; }
	;

regexpSetting [LP property]
@init {
	String message = null;
}
@after {
	if (inPropParseState()) {
		self.setRegexp(property, $exp.val, message);
	}
}
	:	'REGEXP' exp = stringLiteral
		(mess = stringLiteral { message = $mess.val; })?
	;

echoSymbolsSetting [LP property]
@after {
	if (inPropParseState()) {
		self.setEchoSymbols(property);
	}
}
	:	'ECHO'
	;

indexSetting [LP property]
@after {
	if (inPropParseState()) {
		self.addScriptedIndex(property);
	}
}
	:	'INDEXED'
	;

aggPropSetting [LP property]
@after {
	if (inPropParseState()) {
		self.setAggProp(property);
	}
}
	:	'AGGPROP'
	;

notNullDeleteSetting returns [DebugInfo.DebugPoint debugPoint]
@init {
    $debugPoint = getEventDebugPoint();
}
    :   'DELETE'
	;

onEditEventSetting [LP property, List<TypedParameter> context]
@after {
	if (inPropParseState()) {
		self.setScriptedEditAction(property, $et.type, $action.property);
	}
}
	:	'ON' et=formEventType 
		action=listTopContextDependentActionDefinitionBody[context, false, false]
	;

formEventType returns [String type]
	:	'CHANGE' { $type = ServerResponse.CHANGE; }
	|	'CHANGEWYS' { $type = ServerResponse.CHANGE_WYS; }
	|	'EDIT' { $type = ServerResponse.EDIT_OBJECT; }
	|	'GROUPCHANGE' { $type = ServerResponse.GROUP_CHANGE; }
	;

eventIdSetting [LP property]
@after {
	if (inPropParseState()) {
		self.setEventId(property, $id.val);
	}
}
	:	'EVENTID' id=stringLiteral
	;

////////////////////////////////////////////////////////////////////////////////
////////////////////////////////// ACTION PROPERTIES ///////////////////////////
////////////////////////////////////////////////////////////////////////////////

// "multiple inheritance" of topContextDependentActionDefinitionBody
listTopContextDependentActionDefinitionBody[List<TypedParameter> context, boolean dynamic, boolean needFullContext] returns [LPWithParams property]
@after{
    if (inPropParseState()) {
        $property = self.modifyContextFlowActionPropertyDefinitionBodyCreated($property, context, new ArrayList<TypedParameter>(), needFullContext);

        self.topContextActionPropertyDefinitionBodyCreated($property);
    }
}
    :   aDB=listActionDefinitionBody[context, dynamic] { if(inPropParseState()) { $property = $aDB.property; } }
	;

endDeclTopContextDependentActionDefinitionBody[List<TypedParameter> context, boolean dynamic, boolean needFullContext] returns [LPWithParams property]
    :   aDB=topContextDependentActionDefinitionBody[context, dynamic, needFullContext] { $property = $aDB.property; }
	;

// top level, not recursive
topContextDependentActionDefinitionBody[List<TypedParameter> context, boolean dynamic, boolean needFullContext] returns [LPWithParams property]
@after{
    if (inPropParseState()) {
        self.topContextActionPropertyDefinitionBodyCreated($property);
    }
}
    :   aDB=modifyContextFlowActionDefinitionBody[new ArrayList<TypedParameter>(), context, dynamic, needFullContext] { $property = $aDB.property; }
	;

// modifies context + is flow action (uses another actions)
modifyContextFlowActionDefinitionBody[List<TypedParameter> oldContext, List<TypedParameter> newContext, boolean dynamic, boolean needFullContext] returns [LPWithParams property]
@after{
    if (inPropParseState()) {
        $property = self.modifyContextFlowActionPropertyDefinitionBodyCreated($property, newContext, $oldContext, needFullContext);
    }
}
    :	aDB=actionDefinitionBody[newContext, dynamic, true] { $property = $aDB.property; }
	;

keepContextFlowActionDefinitionBody[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property]
    :	aDB=actionDefinitionBody[context, dynamic, false] { $property = $aDB.property; }
	;

actionDefinitionBody[List<TypedParameter> context, boolean dynamic, boolean modifyContext] returns [LPWithParams property]
@init {
	DebugInfo.DebugPoint point = getCurrentDebugPoint();
}
@after{
	if (inPropParseState()) {
		DebugInfo.DebugPoint endPoint = getCurrentDebugPoint(true);
		self.actionPropertyDefinitionBodyCreated($property, point, endPoint, modifyContext, null);
	}
}
	:	(   recDB=recursiveContextActionDB[context, dynamic]	{ $property = $recDB.property; }
	    |	leafDB=leafContextActionDB[context, dynamic]	{ $property = $leafDB.property; }
	    )
	;

// recursive or mixed (in mixed rule there can be semi, but not necessary)
recursiveContextActionDB[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property]
	:	(   extDB=recursiveExtendContextActionDB[context, dynamic]	{ $property = $extDB.property; }
	    |	keepDB=recursiveKeepContextActionDB[context, dynamic]	{ $property = $keepDB.property; }
	    )
;

recursiveExtendContextActionDB[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property]
@init { 
	if (inPropParseState() && dynamic) {
		self.getErrLog().emitExtendActionContextError(self.getParser());
	}
}
	:	forADB=forActionPropertyDefinitionBody[context] { $property = $forADB.property; }
	|	dialogADB=dialogActionDefinitionBody[context] { $property = $dialogADB.property; } // mixed, input
	|	inputADB=inputActionDefinitionBody[context] { $property = $inputADB.property; } // mixed, input
	|	newADB=newActionDefinitionBody[context] { $property = $newADB.property; }
	;

recursiveKeepContextActionDB[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property]
	:	listADB=listActionDefinitionBody[context, dynamic] { $property = $listADB.property; }
	|	confirmADB=confirmActionDefinitionBody[context] { $property = $confirmADB.property; } // mixed, input
	|	newSessionADB=newSessionActionDefinitionBody[context, dynamic] { $property = $newSessionADB.property; }
	|	requestADB=requestActionDefinitionBody[context, dynamic] { $property = $requestADB.property; }
	|	tryADB=tryActionDefinitionBody[context, dynamic] { $property = $tryADB.property; } // mixed
	|	ifADB=ifActionDefinitionBody[context, dynamic] { $property = $ifADB.property; }
	|	caseADB=caseActionDefinitionBody[context, dynamic] { $property = $caseADB.property; }
	|	multiADB=multiActionDefinitionBody[context, dynamic] { $property = $multiADB.property; }	
	|	applyADB=applyActionDefinitionBody[context, dynamic] { $property = $applyADB.property; }
    |   newThreadADB=newThreadActionDefinitionBody[context, dynamic] { $property = $newThreadADB.property; } // mixed
	|	newExecutorADB=newExecutorActionDefinitionBody[context, dynamic] { $property = $newExecutorADB.property; } // mixed, recursive but always semi
;

// always semi in the end
leafContextActionDB[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property]
	:	(   extDB=leafExtendContextActionDB[context, dynamic]	{ $property = $extDB.property; }
	    |	keepDB=leafKeepContextActionDB[context, dynamic]	{ $property = $keepDB.property; }
	    ) ';'
;

leafExtendContextActionDB[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property]
@init {
	if (inPropParseState() && dynamic) {
		self.getErrLog().emitExtendActionContextError(self.getParser());
	}
}
	:	setADB=assignActionDefinitionBody[context] { $property = $setADB.property; }
	|	classADB=changeClassActionDefinitionBody[context] { $property = $classADB.property; }
	|	delADB=deleteActionDefinitionBody[context] { $property = $delADB.property; }
	|	addADB=newWhereActionDefinitionBody[context] { $property = $addADB.property; }
	;

leafKeepContextActionDB[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property]
	:	execADB=execActionDefinitionBody[context, dynamic] { $property = $execADB.property; }
	|	termADB=terminalFlowActionDefinitionBody { $property = $termADB.property; }
	|  	cancelPDB=cancelActionDefinitionBody[context, dynamic] { $property = $cancelPDB.property; }
	|	formADB=formActionDefinitionBody[context, dynamic] { $property = $formADB.property; }
	|	printADB=printActionDefinitionBody[context, dynamic] { $property = $printADB.property; }
	|	exportADB=exportActionDefinitionBody[context, dynamic] { $property = $exportADB.property; }
	|	msgADB=messageActionDefinitionBody[context, dynamic] { $property = $msgADB.property; }
	|	asyncADB=asyncUpdateActionDefinitionBody[context, dynamic] { $property = $asyncADB.property; }
	|	seekADB=seekObjectActionDefinitionBody[context, dynamic] { $property = $seekADB.property; }
	|	mailADB=emailActionDefinitionBody[context, dynamic] { $property = $mailADB.property; }
	|	fileADB=fileActionDefinitionBody[context, dynamic] { $property = $fileADB.property; }
	|	evalADB=evalActionDefinitionBody[context, dynamic] { $property = $evalADB.property; }
	|	drillDownADB=drillDownActionDefinitionBody[context, dynamic] { $property = $drillDownADB.property; }
	|	readADB=readActionDefinitionBody[context, dynamic] { $property = $readADB.property; }
	|	writeADB=writeActionDefinitionBody[context, dynamic] { $property = $writeADB.property; }
	|	importADB=importActionDefinitionBody[context, dynamic] { $property = $importADB.property; }
	|	importFormADB=importFormActionDefinitionBody[context, dynamic] { $property = $importFormADB.property; }
	|	activeFormADB=activeFormActionDefinitionBody[context, dynamic] { $property = $activeFormADB.property; }
	|	activateADB=activateActionDefinitionBody[context, dynamic] { $property = $activateADB.property; }
	|	emptyADB=emptyActionDefinitionBody[context, dynamic] { $property = $emptyADB.property; }
	;

contextIndependentActionDB returns [LP property, List<ResolveClassSet> signature]
@init {
	DebugInfo.DebugPoint point = getCurrentDebugPoint();
	Boolean needToCreateDelegate = null;
}
@after{
	if (inPropParseState()) {
	    LPWithParams lpWithParams = new LPWithParams($property, new ArrayList<Integer>());
		DebugInfo.DebugPoint endPoint = getCurrentDebugPoint(true);
		self.actionPropertyDefinitionBodyCreated(lpWithParams, point, endPoint, false, needToCreateDelegate);

        self.topContextActionPropertyDefinitionBodyCreated(lpWithParams);
	}
}
	:	addformADB=addFormActionDefinitionBody { $property = $addformADB.property; $signature = $addformADB.signature; }
	|	editformADB=editFormActionDefinitionBody { $property = $editformADB.property; $signature = $editformADB.signature; }
	|	customADB=customActionDefinitionBody { $property = $customADB.property; $signature = $customADB.signature; }
    |	abstractActionDef=abstractActionDefinition { $property = $abstractActionDef.property; $signature = $abstractActionDef.signature; needToCreateDelegate = false; } // to debug into implementation immediately, without stepping on abstract declaration
	|	'NATIVE' '(' clist=classIdList ')' { if (inPropParseState()) { $property = null; $signature = self.createClassSetsFromClassNames($clist.ids); }}
	;

mappedForm[List<TypedParameter> context, List<TypedParameter> newContext, boolean dynamic] returns [MappedForm mapped, List<FormActionProps> props = new ArrayList<>()]
@init {
    FormEntity form = null;

    CustomClass mappedCls = null;
    boolean edit = false;
}
	:
	(   (   formName=compoundID { if(inPropParseState()) { form = self.findForm($formName.sid); } } 
	        ('OBJECTS' list=formActionObjectList[form, context, newContext, dynamic] { $props = $list.props; })?
	        {
	            if(inPropParseState())
	                $mapped = MappedForm.create(form, $list.objects != null ? $list.objects : new ArrayList<ObjectEntity>());
	        }
		)
	    |
	    (   ('LIST' | ('EDIT' { edit = true; } ))
	         cls = classId { if(inPropParseState()) { mappedCls = (CustomClass)self.findClass($cls.sid); } }
	         (object=formActionProps["object", mappedCls, context, newContext, dynamic] { $props = Collections.singletonList($object.props); })
	        {
	            if(inPropParseState())
	                $mapped = MappedForm.create(mappedCls, edit);
	        }
	    ))
;


emptyActionDefinitionBody[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property]
@after {
    if (inPropParseState()) {
        $property = new LPWithParams(self.baseLM.getEmpty(), new ArrayList<Integer>());
    }
}
    :
    ;

formActionDefinitionBody[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property]
@init {
	
	Boolean syncType = null;
	WindowFormType windowType = null;

    ManageSessionType manageSession = ManageSessionType.NOMANAGESESSION; // temporary, should be AUTO
	Boolean noCancel = FormEntity.DEFAULT_NOCANCEL; // temporary, should be NULL

	boolean readOnly = false;
	boolean checkOnOk = false;
}
@after {
	if (inPropParseState()) {
		$property = self.addScriptedShowFAProp($mf.mapped, $mf.props, syncType, windowType, manageSession, checkOnOk, noCancel, readOnly);
	}
}
	:	'SHOW' mf=mappedForm[context, null, dynamic]
		(
		    sync = syncTypeLiteral { syncType = $sync.val; }
		|   window = windowTypeLiteral { windowType = $window.val; }

        |	ms=manageSessionClause { manageSession = $ms.result; }
		|	nc=noCancelClause { noCancel = $nc.result; }

		|	'READONLY' { readOnly = true; }
		|	'CHECK' { checkOnOk = true; }
		)*
	;

dialogActionDefinitionBody[List<TypedParameter> context] returns [LPWithParams property]
@init {
	WindowFormType windowType = null;

	List<TypedParameter> newContext = new ArrayList<TypedParameter>(context);

	ManageSessionType manageSession = ManageSessionType.NOMANAGESESSION; // temporary, should be AUTO
	Boolean noCancel = FormEntity.DEFAULT_NOCANCEL; // temporary, should be NULL

	boolean checkOnOk = false;

	boolean readOnly = false;
}
@after {
	if (inPropParseState()) {
		$property = self.addScriptedDialogFAProp($mf.mapped, $mf.props, windowType, manageSession, checkOnOk, noCancel, readOnly, $dDB.property, $dDB.elseProperty, context, newContext);
	}
}
	:	'DIALOG' mf=mappedForm[context, newContext, false]
		(    window = windowTypeLiteral { windowType = $window.val; }

		|	ms=manageSessionClause { manageSession = $ms.result; }
		|	nc=noCancelClause { noCancel = $nc.result; }

		|	'READONLY' { readOnly = true; }
		|	'CHECK' { checkOnOk = true; }
		)*
		dDB=doInputBody[context, newContext]
	;

manageSessionClause returns [ManageSessionType result]
    :       'MANAGESESSION' { $result = ManageSessionType.MANAGESESSION; }
        |   'NOMANAGESESSION' { $result = ManageSessionType.NOMANAGESESSION; }
        |   'MANAGESESSIONX' { $result = ManageSessionType.MANAGESESSIONX; }
        |   'NOMANAGESESSIONX' { $result = ManageSessionType.NOMANAGESESSIONX; }
        |   'AUTOX' { $result = ManageSessionType.AUTO; }
    ;

noCancelClause returns [boolean result]
    :       'CANCEL' { $result = false; }
        |   'NOCANCEL' { $result = true; }
    ;

doInputBody[List<TypedParameter> oldContext, List<TypedParameter> newContext]  returns [LPWithParams property, LPWithParams elseProperty]
        // modifyContextFlowActionDefinitionBody[oldContext, newContext, false, false] - used explicit modifyContextFlowActionDefinitionBodyCreated to support CHANGE clauses
    :	(('DO' dDB=keepContextFlowActionDefinitionBody[newContext, false] { $property = $dDB.property; } ) ('ELSE' eDB=keepContextFlowActionDefinitionBody[newContext, false] { $elseProperty = $eDB.property; } )?)
     | (';' { if(inPropParseState()) { $property = new LPWithParams(self.baseLM.getEmpty(), new ArrayList<Integer>());  } })
;

syncTypeLiteral returns [boolean val]
	:	'WAIT' { $val = true; }
	|	'NOWAIT' { $val = false; }
	;

windowTypeLiteral returns [WindowFormType val]
	:	'FLOAT' { $val = WindowFormType.FLOAT; }
	|	'DOCKED' { $val = WindowFormType.DOCKED; }
	;

printActionDefinitionBody[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property]
@init {
	LPWithParams printerProperty = null;
	FormPrintType printType = null;
    Boolean syncType = null;
    Integer selectTop = null;
}
@after {
	if (inPropParseState()) {
		$property = self.addScriptedPrintFAProp($mf.mapped, $mf.props, printerProperty, printType, $pUsage.propUsage, syncType, selectTop);
	}
}
	:	'PRINT' mf=mappedForm[context, null, dynamic]
		(   ( // static
            (   'XLS'  { printType = FormPrintType.XLS; }
            |	'XLSX' { printType = FormPrintType.XLSX; }
            |	'PDF' { printType = FormPrintType.PDF; }
            |	'DOC'  { printType = FormPrintType.DOC; }
            |	'DOCX' { printType = FormPrintType.DOCX; }
            ) 
            ('TO' pUsage=propertyUsage)?
            )
        |   (
                'MESSAGE' { printType = FormPrintType.MESSAGE; }
                (sync = syncTypeLiteral { syncType = $sync.val; })?
                ('TOP' top = intLiteral { selectTop = $top.val; } )?
            )
        |   ( // static - interactive
		    (sync = syncTypeLiteral { syncType = $sync.val; })?
            ('AUTO' { printType = FormPrintType.AUTO; })?
            ('TO' pe = propertyExpression[context, dynamic] { printerProperty = $pe.property; })?            
            )
        )		
	;
	
exportActionDefinitionBody[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property]
@init {
	FormExportType exportType = null;
	boolean noHeader = false;
    String separator = null;
	String charset = null;
}
@after {
	if (inPropParseState()) {
		$property = self.addScriptedExportFAProp($mf.mapped, $mf.props, exportType, noHeader, separator, charset, $pUsage.propUsage);
	}
}
	:	'EXPORT' mf=mappedForm[context, null, dynamic]
		(	'XML' { exportType = FormExportType.XML; }
	    |  	'JSON' { exportType = FormExportType.JSON; }
		|  	'CSV' { exportType = FormExportType.CSV; } (separatorVal = stringLiteral { separator = $separatorVal.val; })? ('NOHEADER' { noHeader = true; })? ('CHARSET' charsetVal = stringLiteral { charset = $charsetVal.val; })?
	    |  	'DBF' { exportType = FormExportType.DBF; } ('CHARSET' charsetVal = stringLiteral { charset = $charsetVal.val; })?
		)
		('TO' pUsage=propertyUsage)?
	;

initFilterDefinition returns [String propName, List<String> mapping]
	:	'INITFILTER'
		(	pname=ID { $propName = $pname.text; }
	    |	mappedProp=mappedPropertyDraw { $propName = $mappedProp.name; $mapping = $mappedProp.mapping; }
		)
	;

formActionObjectList[FormEntity formEntity, List<TypedParameter> context, List<TypedParameter> newContext, boolean dynamic] returns [List<ObjectEntity> objects = new ArrayList<>(), List<FormActionProps> props = new ArrayList<>() ]
@init {
    ObjectEntity object = null;
}
	:	id=ID { if(inPropParseState()) { object=self.findObjectEntity(formEntity, $id.text); $objects.add(object); } } fap=formActionProps[$id.text, object != null ? object.baseClass : null, context, newContext, dynamic] { $props.add($fap.props); }
		(',' id=ID { if(inPropParseState()) { object=self.findObjectEntity(formEntity, $id.text); $objects.add(object); } } fap=formActionProps[$id.text, object != null ? object.baseClass : null, context, newContext, dynamic] { $props.add($fap.props); })*
	;

formActionProps[String objectName, ValueClass objectClass, List<TypedParameter> context, List<TypedParameter> newContext, boolean dynamic] returns [FormActionProps props]
@init {
    LPWithParams in = null;
    Boolean inNull = false;
    boolean out = false;
    Integer outParamNum = null;
    Boolean outNull = false;
    PropertyUsage outProp = null;

    LPWithParams changeProp = null;

    boolean assign = false;
    boolean constraintFilter = false;

    DebugInfo.DebugPoint assignDebugPoint = null;
}
@after {
    $props = new FormActionProps(in, inNull, out, outParamNum, outNull, outProp, constraintFilter, assign, changeProp, assignDebugPoint);
}
    :   ('=' expr=propertyExpression[context, dynamic] { in = $expr.property; } ('NULL' { inNull = true; } )? )?
        (
            (   'INPUT'
                |
                (
                { assignDebugPoint = getCurrentDebugPoint(); }
                'CHANGE' { assign = true; outNull = true; constraintFilter = true; }
                ('=' consExpr=propertyExpression[context, dynamic])? { changeProp = $consExpr.property; }
                ('NOCONSTRAINTFILTER' { constraintFilter = false; } )?
                ('NOASSIGN' { assign = false; assignDebugPoint = null; } )?
                )
            )
            { out = true; inNull = true; }
            varID=ID?
            { if(newContext!=null && inPropParseState()) { outParamNum = self.getParamIndex(self.new TypedParameter(objectClass, $varID.text != null ? $varID.text : objectName), newContext, true, insideRecursion); } }
            ('NULL' { outNull = true; })?
            ('TO' pUsage=propertyUsage { outProp = $pUsage.propUsage; } )?
            (('CONSTRAINTFILTER' { constraintFilter = true; } ) ('=' consExpr=propertyExpression[context, dynamic] { changeProp = $consExpr.property; } )?)?
        )?
    ;

idEqualPEList[List<TypedParameter> context, boolean dynamic] returns [List<String> ids = new ArrayList<String>(), List<LPWithParams> exprs = new ArrayList<LPWithParams>(), List<Boolean> nulls = new ArrayList<Boolean>()]
@init {
	boolean allowNulls = false;
}
	:	id=ID { $ids.add($id.text); } '=' expr=propertyExpression[context, dynamic] { $exprs.add($expr.property); } { allowNulls = false; } ('NULL' { allowNulls = true; })? { $nulls.add(allowNulls); } 
		(',' id=ID { $ids.add($id.text); } '=' expr=propertyExpression[context, dynamic] { $exprs.add($expr.property); } { allowNulls = false; } ('NULL' { allowNulls = true; })? { $nulls.add(allowNulls); })*
	;
	
customActionDefinitionBody returns [LP property, List<ResolveClassSet> signature]
@init {
	boolean allowNullValue = false;
	List<String> classes = null;
}
@after {
	if (inPropParseState()) {
	    if($code.val == null)
	        $property = self.addScriptedCustomActionProp($classN.val, classes, allowNullValue);
	    else
		    $property = self.addScriptedCustomActionProp($code.val, allowNullValue);
		$signature = (classes == null ? Collections.<ResolveClassSet>nCopies($property.listInterfaces.size(), null) : self.createClassSetsFromClassNames(classes)); 
	}
}
	:	'CUSTOM' 
		(classN = stringLiteral ('(' cls=classIdList ')' { classes = $cls.ids; })? | code = codeLiteral)
	    ('NULL' { allowNullValue = true; })?
	;


addFormActionDefinitionBody returns [LP property, List<ResolveClassSet> signature]
@init {
	FormSessionScope scope = FormSessionScope.NEWSESSION;
}
@after {
	if (inPropParseState()) {
		$signature = new ArrayList<ResolveClassSet>();
		$property = self.addScriptedAddFormAction($cls.sid, scope);
	}
}
	:	'ADDFORM'
		(   'SESSION' { scope = FormSessionScope.OLDSESSION; }
		|   'NESTED'  { scope = FormSessionScope.NESTEDSESSION; }
		)?
		cls=classId
	;

editFormActionDefinitionBody returns [LP property, List<ResolveClassSet> signature]
@init {
	FormSessionScope scope = FormSessionScope.NEWSESSION;
}
@after {
	if (inPropParseState()) {
		$signature = self.createClassSetsFromClassNames(Collections.singletonList($cls.sid));
		$property = self.addScriptedEditFormAction($cls.sid, scope);
	}
}
	:	'EDITFORM'
	    (   'SESSION' { scope = FormSessionScope.OLDSESSION; }
	    |   'NESTED'  { scope = FormSessionScope.NESTEDSESSION; }
        )?
        cls=classId
	;

newWhereActionDefinitionBody[List<TypedParameter> context] returns [LPWithParams property]
@init {
	List<TypedParameter> newContext = new ArrayList<TypedParameter>(context);
	LPWithParams condition = null;
	PropertyUsage toPropUsage = null;
	List<LPWithParams> toPropMapping = null;
}
@after {
	if (inPropParseState()) {
		$property = self.addScriptedAddObjProp(context, $cid.sid, toPropUsage, toPropMapping, condition, newContext);
	}
}
	:	'NEW' cid=classId
		'WHERE' pe=propertyExpression[newContext, true] { condition = $pe.property; }
		('TO' toProp=propertyUsage '(' params=singleParameterList[newContext, false] ')' { toPropUsage = $toProp.propUsage; toPropMapping = $params.props; } )?
	;

newActionDefinitionBody[List<TypedParameter> context] returns [LPWithParams property]
@init {
	List<TypedParameter> newContext = new ArrayList<TypedParameter>(context);

	String varName = "added";
}
@after {
	if (inPropParseState()) {
        $property = self.addScriptedNewAProp(context, $actDB.property, $addObj.paramCnt, $addObj.className, $addObj.autoset, newContext);
	}
}
	:
	    addObj=forAddObjClause[newContext]
//        ('TO' pUsage=propertyUsage)?
   		actDB=modifyContextFlowActionDefinitionBody[context, newContext, false, false]
	;

emailActionDefinitionBody[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property]
@init {
	LPWithParams fromProp = null;
	LPWithParams subjProp = null;
	
	List<Message.RecipientType> recipTypes = new ArrayList<Message.RecipientType>();
	List<LPWithParams> recipProps = new ArrayList<LPWithParams>();

	List<String> forms = new ArrayList<String>();
	List<FormStorageType> formTypes = new ArrayList<FormStorageType>();
	List<OrderedMap<String, LPWithParams>> mapObjects = new ArrayList<OrderedMap<String, LPWithParams>>();
	List<LPWithParams> attachNames = new ArrayList<LPWithParams>();
	List<AttachmentFormat> attachFormats = new ArrayList<AttachmentFormat>();
	List<LPWithParams> attachFileNames = new ArrayList<LPWithParams>();
	List<LPWithParams> attachFiles = new ArrayList<LPWithParams>();
}
@after {
	if (inPropParseState()) {
		$property = self.addScriptedEmailProp(fromProp, subjProp, recipTypes, recipProps, forms, formTypes, mapObjects, attachNames, attachFormats, attachFileNames, attachFiles);
	}
}
	:	'EMAIL'
		('FROM' fromExpr=propertyExpression[context, dynamic] { fromProp = $fromExpr.property; } )?
		'SUBJECT' subjExpr=propertyExpression[context, dynamic] { subjProp = $subjExpr.property; }
		(
			recipType=emailRecipientTypeLiteral { recipTypes.add($recipType.val); }
			recipExpr=propertyExpression[context, dynamic] { recipProps.add($recipExpr.property); }
		)*
		(	(	'INLINE' { formTypes.add(FormStorageType.INLINE); }
				form=compoundID { forms.add($form.sid); attachFormats.add(null); attachNames.add(null); }
				objects=emailActionFormObjects[context, dynamic] { mapObjects.add($objects.mapObjects); }
			)
		|	(	'ATTACH' { formTypes.add(FormStorageType.ATTACH); }
				format=emailAttachFormat { attachFormats.add($format.val); }
				
				{ LPWithParams attachName = null;}
				('NAME' attachNameExpr=propertyExpression[context, dynamic] { attachName = $attachNameExpr.property; } )?
				{ attachNames.add(attachName); }
				
				form=compoundID { forms.add($form.sid); }
				objects=emailActionFormObjects[context, dynamic] { mapObjects.add($objects.mapObjects); }
			)
		|	(	'ATTACH' 'FILE'
				
				attachFile=propertyExpression[context, dynamic] { attachFiles.add($attachFile.property); }
				
				{ LPWithParams attachFileName = null;}
				('NAME' attachFileNameExpr=propertyExpression[context, dynamic] { attachFileName = $attachFileNameExpr.property; } )?
				{ attachFileNames.add(attachFileName); }
			)
		)*
	;
	
emailActionFormObjects[List<TypedParameter> context, boolean dynamic] returns [OrderedMap<String, LPWithParams> mapObjects]
@init {
	$mapObjects = new OrderedMap<String, LPWithParams>();
}

	:	(	'OBJECTS'
			obj=ID '=' objValueExpr=propertyExpression[context, dynamic] { $mapObjects.put($obj.text, $objValueExpr.property); }
			(',' obj=ID '=' objValueExpr=propertyExpression[context, dynamic] { $mapObjects.put($obj.text, $objValueExpr.property); })*
		)?
	;

confirmActionDefinitionBody[List<TypedParameter> context] returns [LPWithParams property]
@init {
    List<TypedParameter> newContext;
    boolean yesNo = false;
}
@after {
	if (inPropParseState()) {
		$property = self.addScriptedConfirmProp($pe.property, $dDB.property, $dDB.elseProperty, yesNo, context, newContext);
	}
}
	:	('CONFIRM' | 'ASK')
        pe=propertyExpression[context, false]
        { newContext = new ArrayList<TypedParameter>(context); }
	    ((varID=ID { if (inPropParseState()) { self.getParamIndex(self.new TypedParameter("BOOLEAN", $varID.text), newContext, true, insideRecursion); } } '=')? 'YESNO' { yesNo = true;} )?
        dDB=doInputBody[context, newContext]
	;
		
messageActionDefinitionBody[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property]
@init {
    boolean noWait = false;
}
@after {
	if (inPropParseState()) {
		$property = self.addScriptedMessageProp($pe.property, noWait);
	}
}
	:	'MESSAGE'
	    ('NOWAIT' { noWait = true; })?
	    pe=propertyExpression[context, dynamic]
	;

asyncUpdateActionDefinitionBody[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property]
@after {
	if (inPropParseState()) {
		$property = self.addScriptedAsyncUpdateProp($pe.property);
	}
}
	:	'ASYNCUPDATE' pe=propertyExpression[context, dynamic]
	;

seekObjectActionDefinitionBody[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property]
@init {
	boolean last = false;
	List<String> objNames = new ArrayList<>();
	List<LPWithParams> lps = new ArrayList<>(); 
}
@after {
	if (inPropParseState()) {
		$property = obj != null ? self.addScriptedObjectSeekProp($obj.sid, $pe.property, last)
		                        : self.addScriptedGroupObjectSeekProp($gobj.sid, objNames, lps, last);
	}
}
	:	'SEEK' ('FIRST' | 'LAST' {last = true; })? 
		(	obj=formObjectID '=' pe=propertyExpression[context, dynamic]
		|	gobj=formGroupObjectID ('OBJECTS' list=seekObjectsList[context, dynamic] { objNames = $list.objects; lps = $list.values; })?
		)
	;

seekObjectsList[List<TypedParameter> context, boolean dynamic] returns [List<String> objects, List<LPWithParams> values] 
	:	list=idEqualPEList[context, dynamic] { $objects = $list.ids; $values = $list.exprs; }
	;

fileActionDefinitionBody[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property]
@init {
	FileActionType actionType = null;
	LPWithParams fileProp = null;
	LPWithParams fileNameProp = null;
	
}
@after {
	if (inPropParseState()) {
		$property = self.addScriptedFileAProp(actionType, fileProp, fileNameProp);
	}
}
	:	(
			'LOADFILE' { actionType = FileActionType.LOAD; } pe=propertyExpression[context, dynamic] { fileProp = $pe.property; }
		| 	'OPEN' { actionType = FileActionType.OPEN; } pe=propertyExpression[context, dynamic] { fileProp = $pe.property; }
		|	'SAVE' { actionType = FileActionType.SAVE; } pe=propertyExpression[context, dynamic] { fileProp = $pe.property; } ('NAME' npe=propertyExpression[context, dynamic] { fileNameProp = $npe.property; })?
		) 
	;

changeClassActionDefinitionBody[List<TypedParameter> context] returns [LPWithParams property]
@init {
	List<TypedParameter> newContext = new ArrayList<TypedParameter>(context);
	LPWithParams condition = null;
}
@after {
	if (inPropParseState()) {
		$property = self.addScriptedChangeClassAProp(context.size(), newContext, $param.property, $className.sid, condition);	
	}
}
	:	'CHANGECLASS' param=singleParameter[newContext, true] 'TO' className=classId 
		('WHERE' pe=propertyExpression[newContext, false] { condition = $pe.property; })?
	;  

deleteActionDefinitionBody[List<TypedParameter> context] returns [LPWithParams property]
@init {
	List<TypedParameter> newContext = new ArrayList<TypedParameter>(context);
	LPWithParams condition = null;
}
@after {
	if (inPropParseState()) {
		$property = self.addScriptedDeleteAProp(context.size(), newContext, $param.property, condition);	
	}
}
	:	'DELETE' param=singleParameter[newContext, true] 
		('WHERE' pe=propertyExpression[newContext, false] { condition = $pe.property; })?
	;  

evalActionDefinitionBody[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property]
@after {
	if (inPropParseState()) {
		$property = self.addScriptedEvalActionProp($expr.property);
	}
}
	:	'EVAL' expr=propertyExpression[context, dynamic]
	;
	
drillDownActionDefinitionBody[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property]
@after {
	if (inPropParseState()) {
		$property = self.addScriptedDrillDownActionProp($expr.property);
	}
}
	:	'DRILLDOWN' expr=propertyExpression[context, dynamic]
	;	

requestActionDefinitionBody[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property]
@after {
	if (inPropParseState()) {
		$property = self.addScriptedRequestAProp($aDB.property, $dDB.property, $eDB.property);
	}
}
	:	'REQUEST' aDB=keepContextFlowActionDefinitionBody[context, dynamic] 'DO' dDB=keepContextFlowActionDefinitionBody[context, dynamic]
	    ('ELSE' eDB=keepContextFlowActionDefinitionBody[context, dynamic])?
	;

inputActionDefinitionBody[List<TypedParameter> context] returns [LPWithParams property]
@init {
	List<TypedParameter> newContext = new ArrayList<TypedParameter>(context);
	boolean assign = false;
	DebugInfo.DebugPoint assignDebugPoint = null;
}
@after {
	if (inPropParseState()) {
		$property = self.addScriptedInputAProp($in.dataClass, $in.initValue, $pUsage.propUsage, $dDB.property, $dDB.elseProperty, context, newContext, assign, assignDebugPoint);
	}
}
	:	'INPUT'
	    in=mappedInput[newContext]
        ( { assignDebugPoint = getCurrentDebugPoint(); } 'CHANGE' { assign = true; } )?
		('TO' pUsage=propertyUsage)?
        dDB=doInputBody[context, newContext]
	;

mappedInput[List<TypedParameter> context] returns [DataClass dataClass, LPWithParams initValue]
@init {
    String varName = null;
}
@after {
	if (inPropParseState()) {
		$dataClass = self.getInputDataClass(varName, context, $ptype.text, $pe.property, insideRecursion);
	    $initValue = $pe.property;
	}
}
    :    
    (
        (varID=ID '=' { varName = $varID.text; } )? 
        ptype=PRIMITIVE_TYPE
    )
    |	
    ( 
        { varName = "object"; } // ?????? ???????????? INPUT =f() CHANGE 
        (varID=ID { varName = $varID.text; } )? 
        '=' pe=propertyExpression[context, false]
    )
;

activeFormActionDefinitionBody[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property]
@after {
	if (inPropParseState()) {
		$property = self.addScriptedActiveFormAProp($name.sid);
	}
}
	:	'ACTIVE' 'FORM' name=compoundID 
	;

activateActionDefinitionBody[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property]
@init {
    FormEntity form = null;
    ComponentView component = null;
    PropertyDrawEntity propertyDraw = null;
}
@after {
	if (inPropParseState()) {
	    if(form != null)
		    $property = self.addScriptedActivateAProp(form, component);
        else
            $property = self.addScriptedFocusActionProp(propertyDraw);
	}
}
	:	'ACTIVATE'
		(	'FORM' fName=compoundID { form = self.findForm($fName.sid); }
		|	'TAB' fc = formComponentID { form = $fc.form; component = $fc.component; }
		|   'PROPERTY' fp = formPropertyID { propertyDraw = $fp.propertyDraw; }
		)
	;

listActionDefinitionBody[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property]
@init {
	List<LPWithParams> props = new ArrayList<LPWithParams>();
	List<LP> localProps = new ArrayList<LP>();
}
@after {
	if (inPropParseState()) {
		$property = self.addScriptedListAProp(props, localProps);
	}
}
	:	'{'
			(	(aDB=keepContextFlowActionDefinitionBody[context, dynamic] { props.add($aDB.property); })
			|	def=localDataPropertyDefinition ';' { localProps.add($def.property); }
			)*
		'}'
	;

nestedPropertiesSelector returns[boolean all = false, List<PropertyUsage> props = new ArrayList<PropertyUsage>()]
    :   'NESTED'
            (   'LOCAL' { $all = true; }
            |   (
            	'(' list=nonEmptyPropertyUsageList { $props = $list.propUsages; } ')'
            	)
            )
    ;
	
localDataPropertyDefinition returns [LP property]
@after {
	if (inPropParseState()) {
		$property = self.addLocalDataProperty($propName.text, $returnClass.sid, $paramClasses.ids, $nlm.nestedType);
	}
}
	:	'LOCAL'
		nlm = nestedLocalModifier
		propName=ID
		'=' returnClass=classId
		'('
			paramClasses=classIdList
		')'
	;

execActionDefinitionBody[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property]
@init {
	boolean isInline = false;
} 
@after {
	if (inPropParseState()) {
		if (isInline) {
			$property = self.addScriptedJoinAProp($iProp.property, $exprList.props);
		} else {
			$property = self.addScriptedJoinAProp($uProp.propUsage, $exprList.props, context);
		}
	}
}
	:	('EXEC')?
		(	uProp=propertyUsage
		|	iProp=inlineProperty { isInline = true; }
		)
		'('
		exprList=propertyExpressionList[context, dynamic]
		')'
	;

assignActionDefinitionBody[List<TypedParameter> context] returns [LPWithParams property]
@init {
	List<TypedParameter> newContext = new ArrayList<TypedParameter>(context); 
	LPWithParams condition = null;
}
@after {
	if (inPropParseState()) {
		$property = self.addScriptedAssignPropertyAProp(context, $propUsage.propUsage, $params.props, $expr.property, condition, newContext);
	}
}
	:	('ASSIGN')?
		propUsage=propertyUsage
		'(' params=singleParameterList[newContext, true] ')'
		'<-'
		expr=propertyExpression[newContext, false] //no need to use dynamic context, because params should be either on global context or used in the left side
		('WHERE'
		whereExpr=propertyExpression[newContext, false] { condition = $whereExpr.property; })?
	;

tryActionDefinitionBody[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property]
@after {
	if (inPropParseState()) {
		$property = self.addScriptedTryAProp($tryADB.property, $finallyADB.property);
	}
}
	:	'TRY' tryADB=keepContextFlowActionDefinitionBody[context, dynamic]
		( 'FINALLY' finallyADB=keepContextFlowActionDefinitionBody[context, dynamic] )?
	;

ifActionDefinitionBody[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property]
@after {
	if (inPropParseState()) {
		$property = self.addScriptedIfAProp($expr.property, $thenADB.property, $elseADB.property);
	}
}
	:	'IF' expr=propertyExpression[context, dynamic] 
		'THEN' thenADB=keepContextFlowActionDefinitionBody[context, dynamic]
		('ELSE' elseADB=keepContextFlowActionDefinitionBody[context, dynamic])?
	;

caseActionDefinitionBody[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property] 
@init {
	List<LPWithParams> whenProps = new ArrayList<LPWithParams>();
	List<LPWithParams> thenActions = new ArrayList<LPWithParams>();
	LPWithParams elseAction = null;
	boolean isExclusive = false;
}
@after {
	if (inPropParseState()) {
		$property = self.addScriptedCaseAProp(whenProps, thenActions, elseAction, isExclusive); 
	}
}
	:	'CASE' (opt=exclusiveOverrideOption { isExclusive = $opt.isExclusive; })?
			( branch=actionCaseBranchBody[context, dynamic] { whenProps.add($branch.whenProperty); thenActions.add($branch.thenAction); } )+
			('ELSE' elseAct=keepContextFlowActionDefinitionBody[context, dynamic] { elseAction = $elseAct.property; })?
	;

actionCaseBranchBody[List<TypedParameter> context, boolean dynamic] returns [LPWithParams whenProperty, LPWithParams thenAction]
	:	'WHEN' whenExpr=propertyExpression[context, dynamic] { $whenProperty = $whenExpr.property; }
		'THEN' thenAct=keepContextFlowActionDefinitionBody[context, dynamic] { $thenAction = $thenAct.property; }
	;

applyActionDefinitionBody[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property]
@init {
	boolean single = false;
	List<PropertyUsage> keepSessionProps = Collections.emptyList();
	boolean keepAllSessionProps = false;
	boolean serializable = false;
}
@after {
	if (inPropParseState()) {
		$property = self.addScriptedApplyAProp($applyADB.property, single, keepSessionProps, keepAllSessionProps, serializable);
	}
}
	:	'APPLY' 
        (mps=nestedPropertiesSelector { keepAllSessionProps = $mps.all; keepSessionProps = $mps.props; })?
        ('SINGLE' { single = true; })?
        ('SERIALIZABLE' { serializable = true; })?
        applyADB=keepContextFlowActionDefinitionBody[context, dynamic]
	;

cancelActionDefinitionBody[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property]
@init {
	List<PropertyUsage> keepSessionProps = Collections.emptyList();
	boolean keepAllSessionProps = false;
}
@after {
	if (inPropParseState()) {
		$property = self.addScriptedCancelAProp(keepSessionProps, keepAllSessionProps);
	}
}
	:	'CANCEL'
        (mps=nestedPropertiesSelector { keepAllSessionProps = $mps.all; keepSessionProps = $mps.props; })?
	;

multiActionDefinitionBody[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property] 
@init {
	boolean isExclusive = true;
}
@after {
	if (inPropParseState()) {
		$property = self.addScriptedMultiAProp($actList.props, isExclusive); 
	}
}
	:	'MULTI' (opt=exclusiveOverrideOption { isExclusive = $opt.isExclusive; })?
		actList=nonEmptyActionPDBList[context, dynamic]
	;

forAddObjClause[List<TypedParameter> context] returns [Integer paramCnt, String className, Boolean autoset = false]
@init {
	String varName = "added";
}
@after {
	if (inPropParseState()) {
		$paramCnt = self.getParamIndex(self.new TypedParameter($className, varName), context, true, insideRecursion);
	}
}
	:	'NEW'
		(varID=ID '=' {varName = $varID.text;})?
		addClass=classId { $className = $addClass.sid; }
        ('AUTOSET' { $autoset = true; } )?
	;

forActionPropertyDefinitionBody[List<TypedParameter> context] returns [LPWithParams property]
@init {
	boolean recursive = false;
	boolean descending = false;
	List<TypedParameter> newContext = new ArrayList<TypedParameter>(context);
	List<LPWithParams> orders = new ArrayList<LPWithParams>();
	Inline inline = null;
	
}
@after {
	if (inPropParseState()) {
		$property = self.addScriptedForAProp(context, $expr.property, orders, $actDB.property, $elseActDB.property, $addObj.paramCnt, $addObj.className, $addObj.autoset, recursive, descending, $in.noInline, $in.forceInline, newContext);
	}	
}
	:	(	'FOR' 
		| 	'WHILE' { recursive = true; }
		)
		expr=propertyExpression[newContext, true]
		('ORDER'
			('DESC' { descending = true; } )? 
			ordExprs=nonEmptyPropertyExpressionList[newContext, false] { orders = $ordExprs.props; }
		)?
		in = inlineStatement[newContext]
		(addObj=forAddObjClause[newContext])?
		'DO' actDB=modifyContextFlowActionDefinitionBody[context, newContext, false, false]
		( {!recursive}?=> 'ELSE' elseActDB=keepContextFlowActionDefinitionBody[context, false])?
	;

terminalFlowActionDefinitionBody returns [LPWithParams property]
@init {
	boolean isBreak = true;
}
@after {
	if (inPropParseState()) {
		$property =	self.getTerminalFlowActionProperty(isBreak);
	}
}
	:	'BREAK'
	|	'RETURN' { isBreak = false; }
	;


////////////////////////////////////////////////////////////////////////////////
/////////////////////////////OVERRIDE STATEMENT/////////////////////////////////
////////////////////////////////////////////////////////////////////////////////

overrideStatement
@init {
	List<TypedParameter> context = new ArrayList<TypedParameter>();
	boolean dynamic = true;
	LPWithParams property = null;
	LPWithParams when = null;
}
@after {
	if (inPropParseState()) {
		self.addImplementationToAbstract($prop.propUsage, $list.params, property, when);
	}
}
	:	prop=propertyUsage
		'(' list=typedParameterList ')' { context = $list.params; dynamic = false; }
		'+='
		('WHEN' whenExpr=propertyExpression[context, dynamic] 'THEN' { when = $whenExpr.property; })?
		(	(expr=propertyExpression[context, dynamic] { property = $expr.property; } ';')
		|	action=listTopContextDependentActionDefinitionBody[context, dynamic, true] { property = $action.property; }
		)
	;

////////////////////////////////////////////////////////////////////////////////
//////////////////////////////// CONSTRAINT STATEMENT //////////////////////////
////////////////////////////////////////////////////////////////////////////////

constraintStatement 
@init {
	boolean checked = false;
	List<PropertyUsage> propUsages = null;
	DebugInfo.DebugPoint debugPoint = null;
	if (inPropParseState()) {
		debugPoint = getEventDebugPoint();
	}
}
@after {
	if (inPropParseState()) {
		self.addScriptedConstraint($expr.property.property, $et.event, checked, propUsages, $message.property.property, debugPoint);
	}
}
	:	'CONSTRAINT'
		et=baseEvent	
		{
			if (inPropParseState()) {
				self.setPrevScope($et.event);
			}
		}
		expr=propertyExpression[new ArrayList<TypedParameter>(), true] { if (inPropParseState()) self.checkNecessaryProperty($expr.property); }
		{
			if (inPropParseState()) {
				self.dropPrevScope($et.event);
			}
		}
		('CHECKED' { checked = true; }
			('BY' list=nonEmptyPropertyUsageList { propUsages = $list.propUsages; })? 
		)?
		'MESSAGE' message=propertyExpression[new ArrayList<TypedParameter>(), false]
		';'
	;


////////////////////////////////////////////////////////////////////////////////
///////////////////////////////// FOLLOWS STATEMENT ////////////////////////////
////////////////////////////////////////////////////////////////////////////////

followsStatement
@init {
	List<TypedParameter> context;
	PropertyUsage mainProp;
	Event event = Event.APPLY;
}
@after {
	if (inPropParseState()) {
		self.addScriptedFollows(mainProp, context, $fcl.pfollows, $fcl.prop, $fcl.event, $fcl.debug);
	}
}
	:	prop=mappedProperty { mainProp = $prop.propUsage; context = $prop.mapping; }
		'=>'
		fcl=followsClause[context] 
		';'
;
	
followsClause[List<TypedParameter> context] returns [LPWithParams prop, Event event = Event.APPLY, DebugInfo.DebugPoint debug, List<PropertyFollowsDebug> pfollows = new ArrayList<PropertyFollowsDebug>()]
@init {
    $debug = getEventDebugPoint();
}
    :	expr = propertyExpression[context, false] 
		('RESOLVE' 
			('LEFT' {$pfollows.add(new PropertyFollowsDebug(true, getEventDebugPoint()));})?
			('RIGHT' {$pfollows.add(new PropertyFollowsDebug(false, getEventDebugPoint()));})?
			et=baseEvent { $event = $et.event; } 
		)? { $prop = $expr.property; }
;

////////////////////////////////////////////////////////////////////////////////
////////////////////////////////// WRITE STATEMENT /////////////////////////////
////////////////////////////////////////////////////////////////////////////////

writeWhenStatement
@init {
    boolean action = false;
}
@after {
	if (inPropParseState()) {
		self.addScriptedWriteWhen($mainProp.propUsage, $mainProp.mapping, $valueExpr.property, $whenExpr.property, action);
	}
}
	:	mainProp=mappedProperty 
		'<-'
		{
			if (inPropParseState()) {
				self.setPrevScope(ChangeEvent.scope);
			}
		}
		valueExpr=propertyExpression[$mainProp.mapping, false]
		'WHEN'
		('DO' { action = true; })? // DO - undocumented syntax
		whenExpr=propertyExpression[$mainProp.mapping, false]
		{
			if (inPropParseState()) {
				self.dropPrevScope(ChangeEvent.scope);
			}
		}
		';'
	;

////////////////////////////////////////////////////////////////////////////////
////////////////////////////////// EVENT STATEMENT /////////////////////////////
////////////////////////////////////////////////////////////////////////////////

eventStatement
@init {
	List<TypedParameter> context = new ArrayList<TypedParameter>();
	List<LPWithParams> orderProps = new ArrayList<LPWithParams>();
	boolean descending = false;
	DebugInfo.DebugPoint debug = null;
	
	if (inPropParseState()) {
		debug = getEventDebugPoint();
	}
}
@after {
	if (inPropParseState()) {
		self.addScriptedEvent($whenExpr.property, $action.property, orderProps, descending, $et.event, $in.noInline, $in.forceInline, debug);
	} 
}
	:	'WHEN'
		et=baseEvent
		{
			if (inPropParseState()) {
				self.setPrevScope($et.event);
			}
		}
		whenExpr=propertyExpression[context, true]
		{
			if (inPropParseState()) {
				self.dropPrevScope($et.event);
			}
		}
		(	'ORDER' ('DESC' { descending = true; })?
			orderList=nonEmptyPropertyExpressionList[context, false] { orderProps.addAll($orderList.props); }
		)?
		in=inlineStatement[context]
		'DO'
		action=endDeclTopContextDependentActionDefinitionBody[context, false, false]
	;

////////////////////////////////////////////////////////////////////////////////
//////////////////////////// GLOBAL EVENT STATEMENT ////////////////////////////
////////////////////////////////////////////////////////////////////////////////

globalEventStatement
@init {
	boolean single = false;
}
@after {
	if (inPropParseState()) {
		self.addScriptedGlobalEvent($action.property, $et.event, single, $property.propUsage);
	}
}
	:	'ON' 
		et=baseEvent
		('SINGLE' { single = true; })?
		('SHOWDEP' property=propertyUsage)?
		{
			if (inPropParseState()) {
				self.setPrevScope($et.event);
			}
		}
		action=endDeclTopContextDependentActionDefinitionBody[new ArrayList<TypedParameter>(), false, false]
		{
			if (inPropParseState()) {
				self.dropPrevScope($et.event);
			}
		}
	;

baseEvent returns [Event event]
@init {
	SystemEvent baseEvent = SystemEvent.APPLY;
	List<String> ids = null;
	List<PropertyUsage> puAfters = null;
}
@after {
	if (inPropParseState()) {
		$event = self.createScriptedEvent(baseEvent, ids, puAfters);
	}
}
	:	('GLOBAL' { baseEvent = SystemEvent.APPLY; } | 'LOCAL' { baseEvent = SystemEvent.SESSION; })?
		('FORMS' (neIdList=nonEmptyCompoundIdList { ids = $neIdList.ids; }) )?
		('GOAFTER' (nePropList=nonEmptyPropertyUsageList { puAfters = $nePropList.propUsages; }) )?
	;

inlineStatement[List<TypedParameter> context] returns [List<LPWithParams> noInline = new ArrayList<LPWithParams>(), boolean forceInline = false]
	:   ('NOINLINE' { $noInline = null; } ( '(' params=singleParameterList[context, false] { $noInline = $params.props; } ')' )? )?
	    ('INLINE' { $forceInline = true; })?
	;

////////////////////////////////////////////////////////////////////////////////
//////////////////////////////// SHOWDEP STATEMENT //////////////////////////////
////////////////////////////////////////////////////////////////////////////////

showDepStatement
@after {
    if (inPropParseState()) {
        self.addScriptedShowDep($property.propUsage, $propFrom.propUsage);
    }
}
    :	'SHOWDEP'
        property=propertyUsage
        'FROM'
        propFrom=propertyUsage
        ';'
    ;

////////////////////////////////////////////////////////////////////////////////
//////////////////////////////// ASPECT STATEMENT //////////////////////////////
////////////////////////////////////////////////////////////////////////////////

aspectStatement
@init {
	List<TypedParameter> context = new ArrayList<TypedParameter>();
	boolean before = true;
}
@after {
	if (inPropParseState()) {
		self.addScriptedAspect($mainProp.propUsage, $mainProp.mapping, $action.property, before);
	}
}
	:	(	'BEFORE' 
		| 	'AFTER' { before = false; }
		)
		mainProp=mappedProperty
		'DO' action=endDeclTopContextDependentActionDefinitionBody[$mainProp.mapping, false, false]
	;


////////////////////////////////////////////////////////////////////////////////
////////////////////////////////// TABLE STATEMENT /////////////////////////////
////////////////////////////////////////////////////////////////////////////////

tableStatement 
@init {
	boolean isFull = false;
}
@after {
	if (inTableParseState()) {
		self.addScriptedTable($name.text, $list.ids, isFull);
	}
}
	:	'TABLE' name=ID '(' list=nonEmptyClassIdList ')' ('FULL' {isFull = true;})? ';';

////////////////////////////////////////////////////////////////////////////////
/////////////////////////////// LOGGABLE STATEMENT /////////////////////////////
////////////////////////////////////////////////////////////////////////////////

loggableStatement
@after {
	if (inPropParseState()) {
		self.addScriptedLoggable($list.propUsages);
	}	
}
	:	'LOGGABLE' list=nonEmptyPropertyUsageList ';'
	;

////////////////////////////////////////////////////////////////////////////////
////////////////////////////////// INDEX STATEMENT /////////////////////////////
////////////////////////////////////////////////////////////////////////////////

// duplicate of singleParameter because of different parse states
singleParameterIndex[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property]
@init {
	String className = null;
}
@after {
	if (inIndexParseState()) {
		$property = new LPWithParams(null, Collections.singletonList(self.getParamIndex(TP(className, $paramName.text), $context, $dynamic, insideRecursion)));
	}
}
	:	(clsId=classId { className = $clsId.sid; })? paramName=parameter
	;

singleParameterIndexList[List<TypedParameter> context, boolean dynamic] returns [List<LPWithParams> props]
@init {
	props = new ArrayList<LPWithParams>();
}
	:	(first=singleParameterIndex[context, dynamic] { props.add($first.property); }
		(',' next=singleParameterIndex[context, dynamic] { props.add($next.property); })*)?
	;


mappedPropertyOrSimpleParamIndex[List<TypedParameter> context] returns [LPWithParams property]
    :   (   toProp=propertyUsage '(' params=singleParameterIndexList[context, true] ')' { if(inIndexParseState()) { $property = self.findIndexProp($toProp.propUsage, $params.props, context); } }
        |   param=singleParameterIndex[context, true] { $property = $param.property; }
        )
;

nonEmptyMappedPropertyOrSimpleParamIndexList[List<TypedParameter> context] returns [List<LPWithParams> props]
@init {
	$props = new ArrayList<LPWithParams>();
}
	:	first=mappedPropertyOrSimpleParamIndex[context] { $props.add($first.property); }
		(',' next=mappedPropertyOrSimpleParamIndex[context] { $props.add($next.property); })*
	;

indexStatement
@init {
	List<TypedParameter> context = new ArrayList<TypedParameter>();
}
@after {
	if (inIndexParseState()) {
		self.addScriptedIndex(context, $list.props);
	}	
}
	:	'INDEX' list=nonEmptyMappedPropertyOrSimpleParamIndexList[context] ';'
	;


////////////////////////////////////////////////////////////////////////////////
/////////////////////////////// WINDOW STATEMENT ////////////////////////////
////////////////////////////////////////////////////////////////////////////////

windowStatement
	:	windowCreateStatement
	|	windowHideStatement
	;

windowCreateStatement
@after {
	if (inPropParseState()) {
		self.addScriptedWindow($type.type, $name.name, $name.caption, $opts.options);
	}
}
	:	'WINDOW' name=simpleNameWithCaption type=windowType opts=windowOptions  ';'
	;

windowHideStatement
	:	'HIDE' 'WINDOW' wid=compoundID ';'
		{
			if (inPropParseState()) {
				self.hideWindow($wid.sid);
			}
		}
	;

windowType returns [WindowType type]
	:	'MENU'		{ $type = MENU; }
	|	'PANEL'		{ $type = PANEL; }
	|	'TOOLBAR'	{ $type = TOOLBAR; }
	|	'TREE'		{ $type = TREE; }
	;

windowOptions returns [NavigatorWindowOptions options]
@init {
	$options = new NavigatorWindowOptions();
}
	:	(	'HIDETITLE' { $options.setDrawTitle(false); }
		|	'DRAWROOT' { $options.setDrawRoot(true); }
		|	'HIDESCROLLBARS' { $options.setDrawScrollBars(false); }
		|	o=orientation { $options.setOrientation($o.val); }
		|	dp=dockPosition { $options.setDockPosition($dp.val); }
		|	bp=borderPosition { $options.setBorderPosition($bp.val); }
		|	'HALIGN' '(' ha=alignmentLiteral ')' { $options.setHAlign($ha.val); }
		|	'VALIGN' '(' va=alignmentLiteral ')' { $options.setVAlign($va.val); }
		|	'TEXTHALIGN' '(' tha=alignmentLiteral ')' { $options.setTextHAlign($tha.val); }
		|	'TEXTVALIGN' '(' tva=alignmentLiteral ')' { $options.setTextVAlign($tva.val); }
		)*
	;

borderPosition returns [BorderPosition val]
	:	'LEFT'		{ $val = BorderPosition.LEFT; }
	|	'RIGHT'		{ $val = BorderPosition.RIGHT; }
	|	'TOP'		{ $val = BorderPosition.TOP; }
	|	'BOTTOM'	{ $val = BorderPosition.BOTTOM; }
	;

dockPosition returns [DockPosition val]
	:	'POSITION' '(' x=intLiteral ',' y=intLiteral ',' w=intLiteral ',' h=intLiteral ')' { $val = new DockPosition($x.val, $y.val, $w.val, $h.val); }
	;

orientation returns [Orientation val]
	:	'VERTICAL'		{ $val = Orientation.VERTICAL; }
	|	'HORIZONTAL'	{ $val = Orientation.HORIZONTAL; }
	;


////////////////////////////////////////////////////////////////////////////////
/////////////////////////////// NAVIGATOR STATEMENT ////////////////////////////
////////////////////////////////////////////////////////////////////////////////

navigatorStatement
	:	'NAVIGATOR' navigatorElementStatementBody[self.baseLM.root]
	;

navigatorElementStatementBody[NavigatorElement parentElement]
	:	'{'
			(	addNavigatorElementStatement[parentElement]
			|	newNavigatorElementStatement[parentElement]
			|	setupNavigatorElementStatement[parentElement]
			|	emptyStatement
			)*
		'}'
	|	emptyStatement
	;

addNavigatorElementStatement[NavigatorElement parentElement]
	:	'ADD' elem=navigatorElementSelector (caption=localizedStringLiteral)? opts=navigatorElementOptions
		{
			if (inPropParseState()) {
				self.setupNavigatorElement($elem.element, $caption.val, $parentElement, $opts.options, true);
			}
		}
		navigatorElementStatementBody[$elem.element]
	;

newNavigatorElementStatement[NavigatorElement parentElement]
@init {
	NavigatorElement newElement = null;
}
	:	'NEW' id=ID (caption=localizedStringLiteral)? ('ACTION' au=propertyUsage)? opts=navigatorElementOptions
		{
			if (inPropParseState()) {
				newElement = self.createScriptedNavigatorElement($id.text, $caption.val, getCurrentDebugPoint(), $parentElement, $opts.options, $au.propUsage);
			}
		}
		navigatorElementStatementBody[newElement]
	;

navigatorElementOptions returns [NavigatorElementOptions options] 
@init {
	$options = new NavigatorElementOptions();
	$options.position = InsertPosition.IN;
}
	:	
	(	'WINDOW' wid=compoundID { $options.windowName = $wid.sid; }
	|	pos=navigatorElementInsertPosition { $options.position = $pos.position; $options.anchor = $pos.anchor; }
	|	'IMAGE' path=stringLiteral { $options.imagePath = $path.val; }	
	)*
	;
	
navigatorElementInsertPosition returns [InsertPosition position, NavigatorElement anchor]
@init {
	$anchor = null;
}
	:	pos=insertRelativePositionLiteral { $position = $pos.val; } elem=navigatorElementSelector { $anchor = $elem.element; }
	|	'FIRST' { $position = InsertPosition.FIRST; }
	;

setupNavigatorElementStatement[NavigatorElement parentElement]
	:	elem=navigatorElementSelector (caption=localizedStringLiteral)? opts=navigatorElementOptions
		{
			if (inPropParseState()) {
				self.setupNavigatorElement($elem.element, $caption.val, $parentElement, $opts.options, false);
			}
		}
		navigatorElementStatementBody[$elem.element]
	;
	
navigatorElementSelector returns [NavigatorElement element]
	:	cid=compoundID
		{
			if (inPropParseState()) {
				$element = self.findNavigatorElement($cid.sid);
			}
		}
	;


////////////////////////////////////////////////////////////////////////////////
////////////////////////////////// DESIGN STATEMENT ////////////////////////////
////////////////////////////////////////////////////////////////////////////////

designStatement
scope {
	ScriptingFormView design;
}
@init {
	ScriptingFormView formView = null;
	boolean applyDefault = false;
}
	:	header=designHeader	{ $designStatement::design = formView = $header.view; }
		componentStatementBody[formView == null ? null : formView.getMainContainer()]
	;

designHeader returns [ScriptingFormView view]
@init {
	boolean customDesign = false;
	LocalizedString caption = null;
}
@after {
	if (inPropParseState()) {
		$view = self.getFormDesign($cid.sid, caption, customDesign);
	}
}
	:	'DESIGN' cid=compoundID (s=localizedStringLiteral { caption = $s.val; })? ('CUSTOM' { customDesign = true; })?
	;

componentStatementBody [ComponentView parentComponent]
	:	'{'
		(	setObjectPropertyStatement[parentComponent]
		|	setupComponentStatement
		|	newComponentStatement[parentComponent]
		|	moveComponentStatement[parentComponent]
		|	removeComponentStatement
		|	emptyStatement
		)*
		'}'
	|	emptyStatement
	;

setupComponentStatement
	:	comp=componentSelector componentStatementBody[$comp.component]
	;

newComponentStatement[ComponentView parentComponent]
@init {
	ComponentView newComp = null;
}
	:	'NEW' cid=ID insPosition=componentInsertPosition
		{
			if (inPropParseState()) {
				newComp = $designStatement::design.createNewComponent($cid.text, parentComponent, $insPosition.position, $insPosition.anchor, self.getVersion());
			}
		}
		componentStatementBody[newComp]
	;
	
moveComponentStatement[ComponentView parentComponent]
@init {
	ComponentView insComp = null;
}
	:	'MOVE' insSelector=componentSelector { insComp = $insSelector.component; } insPosition=componentInsertPosition
		{
			if (inPropParseState()) {
				$designStatement::design.moveComponent(insComp, parentComponent, $insPosition.position, $insPosition.anchor, self.getVersion());
			}
		}
		componentStatementBody[insComp]
	;
	
componentInsertPosition returns [InsertPosition position, ComponentView anchor]
@init {
	$position = InsertPosition.IN;
	$anchor = null;
}
	:	(	(pos=insertRelativePositionLiteral { $position = $pos.val; } comp=componentSelector { $anchor = $comp.component; })
		|	'FIRST' { $position = InsertPosition.FIRST; }
		)?
	;

removeComponentStatement
	:	'REMOVE' compSelector=componentSelector ';'
		{
			if (inPropParseState()) {
				$designStatement::design.removeComponent($compSelector.component, self.getVersion());
			}
		}
	;

componentSelector returns [ComponentView component]
    :
        exc=formComponentSelector[$designStatement::design] { $component = $exc.component; }
    ;
formComponentSelector[ScriptingFormView formView] returns [ComponentView component]
	:	'PARENT' '(' child=componentSelector ')'
		{
			if (inPropParseState()) {
				formView.getParentContainer($child.component, self.getVersion());
			}
		}
	|	'PROPERTY' '(' prop=propertySelector[formView] ')' { $component = $prop.propertyView; }
	|   exc=formContainersComponentSelector
	    {
			if (inPropParseState()) {
				$component = formView.getComponentBySID($exc.sid, self.getVersion());
			}
	    }
	|	mid=ID
		{
			if (inPropParseState()) {
				$component = formView.getComponentBySID($mid.text, self.getVersion());
			}
		}
	;
formContainersComponentSelector returns [String sid]
    :   gt = groupObjectTreeSelector { $sid = $gt.sid; }
    |   gs = globalSingleSelectorType { $sid = $gs.sid; }
    |   'GROUP' '(' (   ggo = ID ',' { $sid = $ggo.text + ".panel.props"; }
                    |   ggo = ID ',' ggr = ID { $sid = $ggo.text + "." + $ggr.text; }
                    |   ggr = ID { $sid = "NOGROUP." + $ggr.text; }
                    |   { $sid = "nogroup.panel.props"; }
                    ) ')'
    |   'FILTERGROUP' '(' ggo = ID ')' { $sid = "filters." + $ggo.text; }
    ;

componentSingleSelectorType returns [String sid]
    :
        ( 'BOX' { $sid = "box"; } | 'TOOLBARBOX' { $sid = "controls"; } | 'TOOLBARLEFT' { $sid = "controls.left"; } |
          'TOOLBARRIGHT' { $sid = "controls.right"; } | 'TOOLBAR' { $sid = "toolbar.props.box"; } | 'PANEL' { $sid = "panel"; }
        )
    ;

globalSingleSelectorType returns [String sid]
    :
        cst=componentSingleSelectorType {
            switch($cst.sid) {
                case "box":
                    $sid = "main";
                    break;
                case "controls":
                    $sid = "functions.box";
                    break;
                case "controls.left":
                    $sid = "leftControls";
                    break;
                case "controls.right":
                    $sid = "rightControls";
                    break;
                case "panel":
                    $sid = "nogroup.panel";
                    break;
                default:
                    $sid = $cst.sid;
            }
        }
    ;

groupObjectTreeSelector returns [String sid]
@init {
	String result = null;
}
    :
        ( cst=componentSingleSelectorType { result = $cst.sid; } | 'TOOLBARSYSTEM' { result = "toolbar"; } |
         'FILTERGROUPS' { result = "filters"; } | 'USERFILTER' { result = "filter"; } | 'GRIDBOX' { result = "grid.box"; } | 'CLASSCHOOSER' { result = "classChooser"; } |
         'GRID' { result = "grid"; } | 'SHOWTYPE' { result = "showType"; }
         )
        '('
        (   'TREE' tg = ID { if(result.equals("grid")) result=""; result = $tg.text + ".tree" + (result.isEmpty()?"":"."+result); }
        |   go = ID { result = $go.text + "." + result; }
        )
        ')'
        { $sid = result; }
    ;


propertySelector[ScriptingFormView formView] returns [PropertyDrawView propertyView = null]
	:	pname=ID
		{
			if (inPropParseState()) {
				$propertyView = formView.getPropertyView($pname.text, self.getVersion());
			}
		}
	|	mappedProp=mappedPropertyDraw	
		{
			if (inPropParseState()) {
				$propertyView = formView.getPropertyView($mappedProp.name, $mappedProp.mapping, self.getVersion());
			}
		}
	;

setObjectPropertyStatement[Object propertyReceiver] returns [String id, Object value]
	:	ID '=' componentPropertyValue ';'  { setObjectProperty($propertyReceiver, $ID.text, $componentPropertyValue.value); }
	;

componentPropertyValue returns [Object value]
	:   c=colorLiteral { $value = $c.val; }
	|   s=stringLiteral { $value = $s.val; }
	|   i=intLiteral { $value = $i.val; }
	|   l=longLiteral { $value = $l.val; }
	|   d=doubleLiteral { $value = $d.val; }
	|   dim=dimensionLiteral { $value = $dim.val; }
	|   b=booleanLiteral { $value = $b.val; }
	|   intB=boundsIntLiteral { $value = $intB.val; }
	|   doubleB=boundsDoubleLiteral { $value = $doubleB.val; }
	|   contType=containerTypeLiteral { $value = $contType.val; }
	|   alignment=flexAlignmentLiteral { $value = $alignment.val; }
	|   calcProp=designCalcPropertyObject { $value = $calcProp.property; }
	;
	
designCalcPropertyObject returns [CalcPropertyObjectEntity property = null]
	:	mProperty=formMappedProperty
		{
			if (inPropParseState()) {
				$property = $designStatement::design.addCalcPropertyObject($mProperty.propUsage, $mProperty.mapping);
			}
		}
	;


////////////////////////////////////////////////////////////////////////////////
////////////////////////////////// META STATEMENT //////////////////////////////
////////////////////////////////////////////////////////////////////////////////

metaCodeDeclarationStatement
@init {
	String code;
	List<String> tokens;
	int lineNumber = self.getParser().getCurrentParserLineNumber(); 
}
@after {
	if (inInitParseState()) {
		self.addScriptedMetaCodeFragment($id.text, $list.ids, tokens, $text, lineNumber);
	}
}
	
	:	'META' id=ID '(' list=idList ')'  
		{
			tokens = self.grabMetaCode($id.text);
		}
		'END'
	;


metaCodeStatement
@init {
	int lineNumber = self.getParser().getCurrentParserLineNumber();
	ScriptParser.State oldState = null; 
	boolean enabledMeta = false;
}
@after {
	self.runMetaCode($id.sid, $list.ids, lineNumber, enabledMeta);
}
	:	'@' id=compoundID '(' list=metaCodeIdList ')' 
		('{' 	
		{ 	enabledMeta = true; 
			if (self.getParser().enterGeneratedMetaState()) {  
				oldState = parseState;
				parseState = ScriptParser.State.GENMETA; 
			}
		}
		statements 
		{ 	if (oldState != null) {
				self.getParser().leaveGeneratedMetaState(); 
				parseState = oldState;
			}
		} 
		'}')? // for intellij plugin
		';'	
	;


metaCodeIdList returns [List<String> ids]
@init {
	ids = new ArrayList<String>();
}
	:		firstId=metaCodeId { ids.add($firstId.sid); }
			( ',' nextId=metaCodeId { ids.add($nextId.sid); })* 
	;


metaCodeId returns [String sid]
	:	id=compoundID 		{ $sid = $id.sid; }
	|	ptype=PRIMITIVE_TYPE	{ $sid = $ptype.text; } 
	|	lit=metaCodeLiteral 	{ $sid = $lit.text; }
	|				{ $sid = ""; }
	;

metaCodeLiteral
	:	STRING_LITERAL 
	| 	UINT_LITERAL
	|	UNUMERIC_LITERAL
	|	UDOUBLE_LITERAL
	|	ULONG_LITERAL
	|	LOGICAL_LITERAL
	|	DATE_LITERAL
	|	DATETIME_LITERAL
	|	TIME_LITERAL
	|	NULL_LITERAL
	|	COLOR_LITERAL
	;


////////////////////////////////////////////////////////////////////////////////
/////////////////////////////////////// COMMON /////////////////////////////////
////////////////////////////////////////////////////////////////////////////////

emptyStatement
	:	';'
	;

mappedProperty returns [PropertyUsage propUsage, List<TypedParameter> mapping]
	:	propU=propertyUsage { $propUsage = $propU.propUsage; }
		'('
		list=typedParameterList { $mapping = $list.params; }
		')'
	;

parameter
	:	ID | RECURSIVE_PARAM
	;

typedParameter returns [TypedParameter param]
@after {
	if (inPropParseState()) {
		$param = self.new TypedParameter($cname.sid, $pname.text);
	}
}
	:	(cname=classId)? pname=ID
	;

simpleNameWithCaption returns [String name, LocalizedString caption] 
	:	simpleName=ID { $name = $simpleName.text; }
		(captionStr=localizedStringLiteral { $caption = $captionStr.val; })?
	;

simpleNameOrWithCaption returns [String name, LocalizedString caption] 
	:	(   simpleName=ID { $name = $simpleName.text; }
		    (captionStr=localizedStringLiteral { $caption = $captionStr.val; })?
        )
        |
            (captionStr=localizedStringLiteral { $caption = $captionStr.val; })
	;
	
idList returns [List<String> ids] 
@init {
	ids = new ArrayList<String>();	
} 
	:	(neIdList=nonEmptyIdList { ids = $neIdList.ids; })?
	;

classIdList returns [List<String> ids]
@init {
	ids = new ArrayList<String>();
}
	:	(neList=nonEmptyClassIdList { ids = $neList.ids; })?
	;

nonEmptyClassIdList returns [List<String> ids]
@init {
	ids = new ArrayList<String>();
}
	:	firstClassName=classId { ids.add($firstClassName.sid); }
		(',' className=classId { ids.add($className.sid); })*
	;

signatureClassList returns [List<String> ids]
@init {
	ids = new ArrayList<String>();
}
	:	(neList=nonEmptySignatureClassList { ids = $neList.ids; })?
	;

nonEmptySignatureClassList returns [List<String> ids]
@init {
	ids = new ArrayList<String>();
}
	:	firstClassName=signatureClass { ids.add($firstClassName.sid); }
		(',' className=signatureClass { ids.add($className.sid); })*
	;

typedParameterList returns [List<TypedParameter> params]
@init {
	params = new ArrayList<TypedParameter>();
}
	:	(neList=nonEmptyTypedParameterList { $params = $neList.params; })?
	;

nonEmptyTypedParameterList returns [List<TypedParameter> params]
@init {
	params = new ArrayList<TypedParameter>();
}
	:	firstParam=typedParameter { params.add($firstParam.param); }
		(',' param=typedParameter { params.add($param.param); })*
	;


compoundIdList returns [List<String> ids] 
@init {
	ids = new ArrayList<String>();	
} 
	:	(neIdList=nonEmptyCompoundIdList { ids = $neIdList.ids; })?
	;

nonEmptyIdList returns [List<String> ids]
@init {
	ids = new ArrayList<String>(); 
}
	:	firstId=ID	{ $ids.add($firstId.text); }
		(',' nextId=ID	{ $ids.add($nextId.text); })*
	;

nonEmptyCompoundIdList returns [List<String> ids]
@init {
	ids = new ArrayList<String>();
}
	:	firstId=compoundID	{ $ids.add($firstId.sid); }
		(',' nextId=compoundID	{ $ids.add($nextId.sid); })*
	;

nonEmptyPropertyUsageList returns [List<PropertyUsage> propUsages]
@init {
	$propUsages = new ArrayList<PropertyUsage>();
}
	:	first=propertyUsage { $propUsages.add($first.propUsage); }
		(',' next=propertyUsage { $propUsages.add($next.propUsage); })* 
	; 

singleParameterList[List<TypedParameter> context, boolean dynamic] returns [List<LPWithParams> props]
@init {
	props = new ArrayList<LPWithParams>();
}
	:	(first=singleParameter[context, dynamic] { props.add($first.property); }
		(',' next=singleParameter[context, dynamic] { props.add($next.property); })*)?
	;

actionPDBList[List<TypedParameter> context, boolean dynamic] returns [List<LPWithParams> props] 
@init {
	$props = new ArrayList<LPWithParams>();
}
	:	(neList=nonEmptyActionPDBList[context, dynamic] { $props = $neList.props; })?
	;

nonEmptyActionPDBList[List<TypedParameter> context, boolean dynamic] returns [List<LPWithParams> props]
@init {
	$props = new ArrayList<LPWithParams>();
}
	:	first=keepContextFlowActionDefinitionBody[context, dynamic] { $props.add($first.property); }
		(',' next=keepContextFlowActionDefinitionBody[context, dynamic] { $props.add($next.property); })*
	; 

propertyExpressionList[List<TypedParameter> context, boolean dynamic] returns [List<LPWithParams> props] 
@init {
	$props = new ArrayList<LPWithParams>();
}
	:	(neList=nonEmptyPropertyExpressionList[context, dynamic] { $props = $neList.props; })?
	;
	

nonEmptyPropertyExpressionList[List<TypedParameter> context, boolean dynamic] returns [List<LPWithParams> props]
@init {
	$props = new ArrayList<LPWithParams>();
}
	:	first=propertyExpression[context, dynamic] { $props.add($first.property); }
		(',' next=propertyExpression[context, dynamic] { $props.add($next.property); })* 
	; 
	
constantProperty returns [LP property]
@init {
	ScriptingLogicsModule.ConstType cls = null;
	Object value = null;
}
@after {
	if (inPropParseState()) {
		$property = self.addConstantProp(cls, value);	
	}
}
	:	lit = literal { cls = $lit.cls; value = $lit.value; }
	;

literal returns [ScriptingLogicsModule.ConstType cls, Object value]
	: 	vint=uintLiteral	{ $cls = ScriptingLogicsModule.ConstType.INT; $value = $vint.val; }
	|	vlong=ulongLiteral	{ $cls = ScriptingLogicsModule.ConstType.LONG; $value = $vlong.val; }
	|	vnum=UNUMERIC_LITERAL	{ $cls = ScriptingLogicsModule.ConstType.NUMERIC; $value = $vnum.text; }
	|	vdouble=udoubleLiteral { $cls = ScriptingLogicsModule.ConstType.REAL; $value = $vdouble.val; }
	|	vstr=localizedStringLiteral	{ $cls = ScriptingLogicsModule.ConstType.STRING; $value = $vstr.val; }  
	|	vbool=booleanLiteral	{ $cls = ScriptingLogicsModule.ConstType.LOGICAL; $value = $vbool.val; }
	|	vdate=dateLiteral	{ $cls = ScriptingLogicsModule.ConstType.DATE; $value = $vdate.val; }
	|	vdatetime=dateTimeLiteral { $cls = ScriptingLogicsModule.ConstType.DATETIME; $value = $vdatetime.val; }
	|	vtime=timeLiteral 	{ $cls = ScriptingLogicsModule.ConstType.TIME; $value = $vtime.val; }
	|	vsobj=staticObjectID { $cls = ScriptingLogicsModule.ConstType.STATIC; $value = $vsobj.sid; }
	|	vnull=NULL_LITERAL 	{ $cls = ScriptingLogicsModule.ConstType.NULL; }
	|	vcolor=colorLiteral { $cls = ScriptingLogicsModule.ConstType.COLOR; $value = $vcolor.val; }		
	;

classId returns [String sid]
	:	id=compoundID { $sid = $id.sid; }
	|	pid=PRIMITIVE_TYPE { $sid = $pid.text; }
	;

signatureClass returns [String sid]
	:	cid=classId { $sid = $cid.sid; }
	|	uc=unknownClass { $sid = $uc.text; }	
	; 

unknownClass 
	:	'?'
	;

typeId returns [String sid]
	:	pid=PRIMITIVE_TYPE { $sid = $pid.text; }
	|	obj='OBJECT' { $sid = $obj.text; }
	;
	
compoundID returns [String sid]
	:	firstPart=ID { $sid = $firstPart.text; } ('.' secondPart=ID { $sid = $sid + '.' + $secondPart.text; })?
	;

staticObjectID returns [String sid]
	:	(namespacePart=ID '.')? classPart=ID '.' namePart=ID { $sid = ($namespacePart != null ? $namespacePart.text + '.' : "") + $classPart.text + '.' + $namePart.text; }
	;

formGroupObjectID returns [String sid]
    :	(namespacePart=ID '.')? formPart=ID '.' namePart=ID { $sid = ($namespacePart != null ? $namespacePart.text + '.' : "") + $formPart.text + '.' + $namePart.text; }
    ;

formObjectID returns [String sid]
    :	(namespacePart=ID '.')? formPart=ID '.' namePart=ID { $sid = ($namespacePart != null ? $namespacePart.text + '.' : "") + $formPart.text + '.' + $namePart.text; }
    ;

formComponentID returns [FormEntity form, ComponentView component]
@init {
	ScriptingFormView formView = null;
}
    :
        (namespacePart=ID '.')? formPart=ID '.'
        {
            if(inPropParseState()) {
                formView = self.getFormDesign(($namespacePart != null ? $namespacePart.text + '.' : "") + $formPart.text, null, false);
            }
        }
        cs = formComponentSelector[formView] { $component = $cs.component; }
        {
            if(inPropParseState()) {
                $form = formView.getView().entity;
            }
        }
    ;

formPropertyID returns [PropertyDrawEntity propertyDraw]
@init {
	FormEntity form = null;
}
    :
        (namespace=ID '.')? formSName=ID '.'
        {
            if(inPropParseState()) {
                form = self.findForm(($namespace == null ? "" : $namespace.text + ".") + $formSName.text);
            }
        }
        prop=formPropertySelector[form]
        {
            $propertyDraw = $prop.propertyDraw;
        }
    ;

multiCompoundID returns [String sid]
	:	id=ID { $sid = $id.text; } ('.' cid=ID { $sid = $sid + '.' + $cid.text; } )*
	;

exclusiveOverrideOption returns [boolean isExclusive]
	:	'OVERRIDE' { $isExclusive = false; }
	|	'EXCLUSIVE'{ $isExclusive = true; } 
	;

abstractExclusiveOverrideOption returns [boolean isExclusive, Boolean isLast = null]
	:	('OVERRIDE' { $isExclusive = false; } (acopt = abstractCaseAddOption {$isLast = $acopt.isLast; } )? )
	|	'EXCLUSIVE'{ $isExclusive = true; }
	;

abstractCaseAddOption returns [boolean isLast]
	:	'FIRST' { $isLast = false; }
	|	'LAST'{ $isLast = true; }
	;

colorLiteral returns [Color val]
	:	c=COLOR_LITERAL { $val = Color.decode($c.text); }
	|	'RGB' '(' r=uintLiteral ',' g=uintLiteral ',' b=uintLiteral ')' { $val = self.createScriptedColor($r.val, $g.val, $b.val); } 
	;

stringLiteral returns [String val]
	:	s=STRING_LITERAL { $val = self.transformStringLiteral($s.text); }
	;

localizedStringLiteral returns [LocalizedString val]
	:	s=STRING_LITERAL { $val = self.transformLocalizedStringLiteral($s.text); } 
	;

intLiteral returns [int val]
@init {
	boolean isMinus = false;
}
	:	(MINUS {isMinus=true;})?
		ui=uintLiteral  { $val = isMinus ? -$ui.val : $ui.val; }
	;

longLiteral returns [long val]
@init {
	boolean isMinus = false;
} 
	:	(MINUS {isMinus = true;})?
		ul=ulongLiteral { $val = isMinus ? -$ul.val : $ul.val; } 
	;	

doubleLiteral returns [double val]
@init {
	boolean isMinus = false;
}
	:	(MINUS {isMinus=true;})?
		ud=UNUMERIC_LITERAL { $val = self.createScriptedDouble($ud.text); }
		{ if (isMinus) $val = -$val; }
	;

dateLiteral returns [java.sql.Date val]
	:	date=DATE_LITERAL { $val = self.dateLiteralToDate($date.text); }
	;

dateTimeLiteral returns [java.sql.Timestamp val]
	:	time=DATETIME_LITERAL { $val = self.dateTimeLiteralToTimestamp($time.text); }
	;

timeLiteral returns [java.sql.Time val]
	:	time=TIME_LITERAL { $val = self.timeLiteralToTime($time.text); }
	;

booleanLiteral returns [boolean val]
	:	bool=LOGICAL_LITERAL { $val = Boolean.valueOf($bool.text); }
	;

dimensionLiteral returns [Dimension val]
	:	'(' x=intLiteral ',' y=intLiteral ')' { $val = new Dimension($x.val, $y.val); }
	;

boundsIntLiteral returns [Insets val]
	:	'(' top=intLiteral ',' left=intLiteral ',' bottom=intLiteral ',' right=intLiteral ')' { $val = new Insets($top.val, $left.val, $bottom.val, $right.val); }
	;

boundsDoubleLiteral returns [Bounds val]
	:	'(' top=doubleLiteral ',' left=doubleLiteral ',' bottom=doubleLiteral ',' right=doubleLiteral ')' { $val = new Bounds($top.val, $left.val, $bottom.val, $right.val); }
	;

codeLiteral returns [String val]
	:	s=CODE_LITERAL { $val = self.transformStringLiteral($s.text); }
	;

insertRelativePositionLiteral returns [InsertPosition val]
	:	'BEFORE' { $val = InsertPosition.BEFORE; }
	|	'AFTER' { $val = InsertPosition.AFTER; }
	;

containerTypeLiteral returns [ContainerType val]
	:	'CONTAINERV' { $val = ContainerType.CONTAINERV; }	
	|	'CONTAINERH' { $val = ContainerType.CONTAINERH; }	
	|	'COLUMNS' { $val = ContainerType.COLUMNS; }
	|	'TABBED' { $val = ContainerType.TABBED_PANE; }
	|	'SPLITH' { $val = ContainerType.HORIZONTAL_SPLIT_PANE; }
	|	'SPLITV' { $val = ContainerType.VERTICAL_SPLIT_PANE; }
	|   'SCROLL' { $val = ContainerType.SCROLL; }
	;

alignmentLiteral returns [Alignment val]
    :   'LEADING' { $val = Alignment.LEADING; }
    |   'CENTER' { $val = Alignment.CENTER; }
    |   'TRAILING' { $val = Alignment.TRAILING; }
    ;

flexAlignmentLiteral returns [FlexAlignment val]
    :   'LEADING' { $val = FlexAlignment.LEADING; }
    |   'CENTER' { $val = FlexAlignment.CENTER; }
    |   'TRAILING' { $val = FlexAlignment.TRAILING; }
    |   'STRETCH' { $val = FlexAlignment.STRETCH; }
    ;

propertyEditTypeLiteral returns [PropertyEditType val]
	:	'EDITABLE' { $val = PropertyEditType.EDITABLE; }
	|	'READONLY' { $val = PropertyEditType.READONLY; }
	|	'SELECTOR' { $val = PropertyEditType.SELECTOR; }
	;

emailRecipientTypeLiteral returns [Message.RecipientType val]
	:	'TO'	{ $val = Message.RecipientType.TO; }
	|	'CC'	{ $val = Message.RecipientType.CC; }
	|	'BCC'	{ $val = Message.RecipientType.BCC; }
	;
	
emailAttachFormat returns [AttachmentFormat val]
	:	'PDF'	{ $val = AttachmentFormat.PDF; }
	|	'DOCX'	{ $val = AttachmentFormat.DOCX; }
	|	'HTML'	{ $val = AttachmentFormat.HTML; }
	|	'RTF'	{ $val = AttachmentFormat.RTF; }
	|	'XLSX'	{ $val = AttachmentFormat.XLSX; }
	|	'DBF'	{ $val = AttachmentFormat.DBF; }
	;

udoubleLiteral returns [double val]
	:	d=UDOUBLE_LITERAL { $val = self.createScriptedDouble($d.text.substring(0, $d.text.length() - 1)); }
	;	
		
uintLiteral returns [int val]
	:	u=UINT_LITERAL { $val = self.createScriptedInteger($u.text); }
	;		

ulongLiteral returns [long val]
	:	u=ULONG_LITERAL { $val = self.createScriptedLong($u.text.substring(0, $u.text.length() - 1)); }
	;

relOperand 
	:	RELEQ_OPERAND | LESS_OPERAND | GR_OPERAND	
	;
	
multOperand
	:	MULT | DIV
	;

/////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////// LEXER //////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////////
	
fragment NEWLINE	:	'\r'?'\n'; 
fragment SPACE		:	(' '|'\t');
fragment STR_LITERAL_CHAR	: '\\\'' | '\\\\' | ~('\r'|'\n'|'\'');	 // overcomplicated due to bug in ANTLR Works
fragment DIGIT		:	'0'..'9';
fragment DIGITS		:	('0'..'9')+;
fragment EDIGITS	:	('0'..'9')*;
fragment HEX_DIGIT	: 	'0'..'9' | 'a'..'f' | 'A'..'F';
fragment FIRST_ID_LETTER	: ('a'..'z'|'A'..'Z');
fragment NEXT_ID_LETTER		: ('a'..'z'|'A'..'Z'|'_'|'0'..'9');
fragment OPEN_CODE_BRACKET	: '<{';
fragment CLOSE_CODE_BRACKET : '}>';

PRIMITIVE_TYPE  :	'INTEGER' | 'DOUBLE' | 'LONG' | 'BOOLEAN' | 'DATE' | 'DATETIME' | 'YEAR' | 'TEXT'  | 'RICHTEXT' | 'TIME' | 'WORDFILE' | 'IMAGEFILE' | 'PDFFILE' | 'CUSTOMFILE' | 'EXCELFILE' | 'WORDLINK' | 'IMAGELINK' | 'PDFLINK' | 'CUSTOMLINK' | 'EXCELLINK' | 'STRING[' DIGITS ']' | 'ISTRING[' DIGITS ']'  | 'VARSTRING[' DIGITS ']' | 'VARISTRING[' DIGITS ']' | 'NUMERIC[' DIGITS ',' DIGITS ']' | 'COLOR';
LOGICAL_LITERAL :	'TRUE' | 'FALSE';
NULL_LITERAL	:	'NULL';	
ID				:	FIRST_ID_LETTER NEXT_ID_LETTER*;
WS				:	(NEWLINE | SPACE) { $channel=HIDDEN; };
STRING_LITERAL	:	'\'' STR_LITERAL_CHAR* '\'';
COLOR_LITERAL 	:	'#' HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT;
COMMENTS		:	('//' .* '\n') { $channel=HIDDEN; };
UINT_LITERAL 	:	DIGITS;
ULONG_LITERAL	:	DIGITS('l'|'L');
UDOUBLE_LITERAL	:	DIGITS '.' EDIGITS('d'|'D');
UNUMERIC_LITERAL:	DIGITS '.' EDIGITS;	  
DATE_LITERAL	:	DIGIT DIGIT DIGIT DIGIT '_' DIGIT DIGIT '_' DIGIT DIGIT; 
DATETIME_LITERAL:	DIGIT DIGIT DIGIT DIGIT '_' DIGIT DIGIT '_' DIGIT DIGIT '_' DIGIT DIGIT ':' DIGIT DIGIT;	
TIME_LITERAL	:	DIGIT DIGIT ':' DIGIT DIGIT;
RECURSIVE_PARAM :	'$' FIRST_ID_LETTER NEXT_ID_LETTER*;	
EQ_OPERAND		:	('==') | ('!=');
LESS_OPERAND	: 	('<');
GR_OPERAND		:	('>');
RELEQ_OPERAND	: 	('<=') | ('>=');
MINUS			:	'-';
PLUS			:	'+';
MULT			:	'*';
DIV				:	'/';
ADDOR_OPERAND	:	'(+)' | {ahead("(-)")}?=> '(-)';
CONCAT_OPERAND	:	'##';
CONCAT_CAPITALIZE_OPERAND	:	'###';
CODE_LITERAL    : OPEN_CODE_BRACKET .* CLOSE_CODE_BRACKET;