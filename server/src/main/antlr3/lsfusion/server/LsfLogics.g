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
	import lsfusion.interop.FormPrintType;
	import lsfusion.interop.ModalityType;
	import lsfusion.server.form.instance.FormSessionScope;
	import lsfusion.server.data.Union;
	import lsfusion.server.data.expr.query.PartitionType;
	import lsfusion.server.form.entity.*;
	import lsfusion.server.form.navigator.NavigatorElement;
	import lsfusion.server.form.view.ComponentView;
	import lsfusion.server.form.view.GroupObjectView;
	import lsfusion.server.form.view.PropertyDrawView;
	import lsfusion.server.classes.sets.ResolveClassSet;
	import lsfusion.server.logics.mutables.Version;
	import lsfusion.server.logics.linear.LP;
	import lsfusion.server.logics.linear.LCP;
	import lsfusion.server.logics.property.PropertyFollows;
	import lsfusion.server.logics.property.Cycle;
	import lsfusion.server.logics.scripted.*;
	import lsfusion.server.logics.scripted.ScriptingLogicsModule.WindowType;
	import lsfusion.server.logics.scripted.ScriptingLogicsModule.InsertPosition;
	import lsfusion.server.logics.scripted.ScriptingLogicsModule.GroupingType;
	import lsfusion.server.logics.scripted.ScriptingLogicsModule.LPWithParams;
	import lsfusion.server.logics.scripted.ScriptingLogicsModule.TypedParameter;
	import lsfusion.server.logics.scripted.ScriptingLogicsModule.PropertyUsage;
	import lsfusion.server.logics.scripted.ScriptingFormEntity.RegularFilterInfo;
	import lsfusion.server.mail.SendEmailActionProperty.FormStorageType;
	import lsfusion.server.mail.AttachmentFormat;
	import lsfusion.server.logics.property.actions.flow.Inline;
	import lsfusion.server.logics.property.actions.SystemEvent;
	import lsfusion.server.logics.property.Event;
	import lsfusion.server.logics.property.actions.flow.ListCaseActionProperty;
	import lsfusion.server.logics.property.CaseUnionProperty;
	import lsfusion.server.logics.property.IncrementType;
	import lsfusion.server.data.expr.formula.SQLSyntaxType;
	import lsfusion.server.logics.property.BooleanDebug;
	import lsfusion.server.logics.property.PropertyFollowsDebug;
	import lsfusion.server.logics.debug.ActionDebugInfo;
	
	import javax.mail.Message;

	import lsfusion.server.form.entity.GroupObjectProp;

	import lsfusion.base.col.interfaces.immutable.ImSet;

	import java.util.*;
	import java.awt.*;
	import org.antlr.runtime.BitSet;
	import java.util.List;
	import java.sql.Date;

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

	public MappedProperty getPropertyWithMapping(PropertyUsage pUsage, List<String> mapping) throws ScriptingErrorLog.SemanticErrorException {
		if (inPropParseState()) {
			return $formStatement::form.getPropertyWithMapping(pUsage, mapping);
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
	List<String> instanceNames = new ArrayList<String>();
	List<String> instanceCaptions = new ArrayList<String>();
}
@after {
	if (inClassParseState()) {
	    if(!isNative)
		    self.addScriptedClass($nameCaption.name, $nameCaption.caption, isAbstract, $classData.names, $classData.captions, $classData.parents);
	}
}
	:	'CLASS' ('ABSTRACT' {isAbstract = true;} | 'NATIVE' {isNative = true;})?
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

classInstancesAndParents returns [List<String> names, List<String> captions, List<String> parents] 
@init {
	$parents = new ArrayList<String>();
	$names = new ArrayList<String>();
	$captions = new ArrayList<String>();
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
}
@after {
	if (inPropParseState() && initialDeclaration) {
		self.addScriptedForm($formStatement::form);
	}
}
	:	(	declaration=formDeclaration { $formStatement::form = $declaration.form; initialDeclaration = true; }
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
		|	listFormDeclaration
		)*
		';'
	;

dialogFormDeclaration
	:	'DIALOG' cid=classId 'OBJECT' oid=ID
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
	
listFormDeclaration
	:	'LIST' cid=classId 'OBJECT' oid=ID
		{
			if (inPropParseState()) {
				$formStatement::form.setAsListForm($cid.sid, $oid.text, self.getVersion());
			}
		}
	;

formDeclaration returns [ScriptingFormEntity form]
@init {
	ModalityType modalityType = null;
	int autoRefresh = 0;
	boolean keepSessionProperties = false;
}
@after {
	if (inPropParseState()) {
		$form = self.createScriptedForm($formNameCaption.name, $formNameCaption.caption, $title.val, $img.val, modalityType, autoRefresh, keepSessionProperties);
	}
}
	:	'FORM' 
		formNameCaption=simpleNameWithCaption
		('TITLE' title=stringLiteral)?
		(modality = modalityTypeLiteral { modalityType = $modality.val; })?
		('IMAGE' img=stringLiteral)?
		('AUTOREFRESH' refresh=intLiteral { autoRefresh = $refresh.val; })?
		('KEEPSESSIONPROPERTIES' { keepSessionProperties = true; })?
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
	:	(object = formCommonGroupObject { $groupObject = $object.groupObject; })	
		(path = formGroupObjectReportPath { $groupObject.setReportPathProp($path.propUsage, $path.mapping); })?
		(viewType = formGroupObjectViewType { $groupObject.setViewType($viewType.type, $viewType.isInitType); } )?
		(pageSize = formGroupObjectPageSize { $groupObject.setPageSize($pageSize.value); })?
	; 

formTreeGroupObjectDeclaration returns [ScriptingGroupObject groupObject, List<PropertyUsage> properties]
	:	(object = formCommonGroupObject { $groupObject = $object.groupObject; })
		(parent = treeGroupParentDeclaration { $properties = $parent.properties; })?
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

formGroupObjectReportPath returns [PropertyUsage propUsage, List<String> mapping]
	:	'REPORTFILE' prop=formMappedProperty { $propUsage = $prop.propUsage; $mapping = $prop.mapping; }
	;

formGroupObjectViewType returns [ClassViewType type, boolean isInitType]
	: 	('INIT' {$isInitType = true;} | 'FIXED' {$isInitType = false;})
		viewType=classViewType { $type = $viewType.type; }
	;

classViewType returns [ClassViewType type]
	: 	('PANEL' {$type = ClassViewType.PANEL;} | 'HIDE' {$type = ClassViewType.HIDE;} | 'GRID' {$type = ClassViewType.GRID;})
	;

formGroupObjectPageSize returns [Integer value = null]
	:	'PAGESIZE' size = intLiteral { $value = $size.val; }
	;

formSingleGroupObjectDeclaration returns [String name, String className, String caption, ActionPropertyObjectEntity event] 
	:	foDecl=formObjectDeclaration { $name = $foDecl.name; $className = $foDecl.className; $caption = $foDecl.caption; $event = $foDecl.event; }
	;

formMultiGroupObjectDeclaration returns [String groupName, List<String> objectNames, List<String> classNames, List<String> captions, List<ActionPropertyObjectEntity> events]
@init {
	$objectNames = new ArrayList<String>();
	$classNames = new ArrayList<String>();
	$captions = new ArrayList<String>();
	$events = new ArrayList<ActionPropertyObjectEntity>();
}
	:	(gname=ID { $groupName = $gname.text; } '=')?
		'('
			objDecl=formObjectDeclaration { $objectNames.add($objDecl.name); $classNames.add($objDecl.className); $captions.add($objDecl.caption); $events.add($objDecl.event); }
			(',' objDecl=formObjectDeclaration { $objectNames.add($objDecl.name); $classNames.add($objDecl.className); $captions.add($objDecl.caption); $events.add($objDecl.event); })+
		')'
	;


formObjectDeclaration returns [String name, String className, String caption, ActionPropertyObjectEntity event] 
	:	(objectName=ID { $name = $objectName.text; } '=')?	
		id=classId { $className = $id.sid; }
		(c=stringLiteral { $caption = $c.val; })?
		('ON' 'CHANGE' faprop=formActionPropertyObject { $event = $faprop.action; })?
	; 
	
formPropertiesList
@init {
	List<PropertyUsage> properties = new ArrayList<PropertyUsage>();
	List<String> aliases = new ArrayList<String>();
	List<List<String>> mapping = new ArrayList<List<String>>();
	FormPropertyOptions commonOptions = null;
	List<FormPropertyOptions> options = new ArrayList<FormPropertyOptions>();
}
@after {
	if (inPropParseState()) {
		$formStatement::form.addScriptedPropertyDraws(properties, aliases, mapping, commonOptions, options, self.getVersion());
	}
}
	:	'PROPERTIES' '(' objects=idList ')' opts=formPropertyOptionsList list=formPropertyUList
		{
			commonOptions = $opts.options;
			properties = $list.properties;
			aliases = $list.aliases;
			mapping = Collections.nCopies(properties.size(), $objects.ids);
			options = $list.options;
		}
	|	'PROPERTIES' opts=formPropertyOptionsList mappedList=formMappedPropertiesList
		{
			commonOptions = $opts.options;
			properties = $mappedList.properties;
			aliases = $mappedList.aliases;
			mapping = $mappedList.mapping;
			options = $mappedList.options;
		}
	;	

formPropertyOptionsList returns [FormPropertyOptions options]
@init {
	$options = new FormPropertyOptions();
}
	:	(	editType = propertyEditTypeLiteral { $options.setEditType($editType.val); }
		|	'HINTNOUPDATE' { $options.setHintNoUpdate(true); }
		|	'HINTTABLE' { $options.setHintTable(true); }
		|	'TOOLBAR' { $options.setDrawToToolbar(true); }
		|	'OPTIMISTICASYNC' { $options.setOptimisticAsync(true); }
		|	'COLUMNS' (columnsName=stringLiteral)? '(' ids=nonEmptyIdList ')' { $options.setColumns($columnsName.text, getGroupObjectsList($ids.ids, self.getVersion())); }
		|	'SHOWIF' propObj=formCalcPropertyObject { $options.setShowIf($propObj.property); }
		|	'READONLYIF' propObj=formCalcPropertyObject { $options.setReadOnlyIf($propObj.property); }
		|	'BACKGROUND' propObj=formCalcPropertyObject { $options.setBackground($propObj.property); }
		|	'FOREGROUND' propObj=formCalcPropertyObject { $options.setForeground($propObj.property); }
		|	'HEADER' propObj=formCalcPropertyObject { $options.setHeader($propObj.property); }
		|	'FOOTER' propObj=formCalcPropertyObject { $options.setFooter($propObj.property); }
		|	'FORCE' viewType=classViewType { $options.setForceViewType($viewType.type); }
		|	'TODRAW' toDraw=formGroupObjectEntity { $options.setToDraw($toDraw.groupObject); }
		|	'BEFORE' pdraw=formPropertyDraw { $options.setNeighbourPropertyDraw($pdraw.property, $pdraw.text); $options.setNeighbourType(false); }
		|	'AFTER'  pdraw=formPropertyDraw { $options.setNeighbourPropertyDraw($pdraw.property, $pdraw.text); $options.setNeighbourType(true); }
		|	'QUICKFILTER' pdraw=formPropertyDraw { $options.setQuickFilterPropertyDraw($pdraw.property); }
		|	'ON' 'EDIT' prop=formActionPropertyObject { $options.addEditAction(ServerResponse.EDIT_OBJECT, $prop.action); }
		|	'ON' 'CHANGE' prop=formActionPropertyObject { $options.addEditAction(ServerResponse.CHANGE, $prop.action); }
		|	'ON' 'CHANGEWYS' prop=formActionPropertyObject { $options.addEditAction(ServerResponse.CHANGE_WYS, $prop.action); }
		|	'ON' 'SHORTCUT' (c=stringLiteral)? prop=formActionPropertyObject { $options.addContextMenuEditAction($c.val, $prop.action); }
		|	'EVENTID' id=stringLiteral { $options.setEventId($id.val); }
		)*
	;

formPropertyDraw returns [PropertyDrawEntity property]
	:	id=ID              	{ if (inPropParseState()) $property = $formStatement::form.getPropertyDraw($id.text, self.getVersion()); }
	|	prop=mappedPropertyDraw { if (inPropParseState()) $property = $formStatement::form.getPropertyDraw($prop.name, $prop.mapping, self.getVersion()); }
	;

formMappedPropertiesList returns [List<String> aliases, List<PropertyUsage> properties, List<List<String>> mapping, List<FormPropertyOptions> options]
@init {
	$aliases = new ArrayList<String>();
	$properties = new ArrayList<PropertyUsage>();
	$mapping = new ArrayList<List<String>>();
	$options = new ArrayList<FormPropertyOptions>();
	String alias = null;
}
	:	{ alias = null; }
		(id=ID '=' { alias = $id.text; })?
		mappedProp=formMappedProperty opts=formPropertyOptionsList
		{
			$aliases.add(alias);
			$properties.add($mappedProp.propUsage);
			$mapping.add($mappedProp.mapping);
			$options.add($opts.options);
		}
		(','
			{ alias = null; }
			(id=ID '=' { alias = $id.text; })?
			mappedProp=formMappedProperty opts=formPropertyOptionsList
			{
				$aliases.add(alias);
				$properties.add($mappedProp.propUsage);
				$mapping.add($mappedProp.mapping);
				$options.add($opts.options);
			}
		)*
	;

formCalcPropertyObject returns [CalcPropertyObjectEntity property = null]
	:	mProperty=formMappedProperty
		{
			if (inPropParseState()) {
				$property = $formStatement::form.addCalcPropertyObject($mProperty.propUsage, $mProperty.mapping);
			}
		}
	;

formActionPropertyObject returns [ActionPropertyObjectEntity action = null]
	:	mProperty=formMappedProperty
		{
			if (inPropParseState()) {
				$action = $formStatement::form.addActionPropertyObject($mProperty.propUsage, $mProperty.mapping);
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

formPropertyUList returns [List<String> aliases, List<PropertyUsage> properties, List<FormPropertyOptions> options]
@init {
	$aliases = new ArrayList<String>();
	$properties = new ArrayList<PropertyUsage>();
	$options = new ArrayList<FormPropertyOptions>();
	String alias = null;
}
	:	{ alias = null; }
		(id=ID '=' { alias = $id.text; })?
		pu=formPropertyUsage opts=formPropertyOptionsList
		{
			$aliases.add(alias);
			$properties.add($pu.propUsage);
			$options.add($opts.options);
		}
		(','
			{ alias = null; }
			(id=ID '=' { alias = $id.text; })?
			pu=formPropertyUsage opts=formPropertyOptionsList
			{
				$aliases.add(alias);
				$properties.add($pu.propUsage);
				$options.add($opts.options);
			}
		)*
	;


formPropertyUsage returns [PropertyUsage propUsage]
	:	pu=propertyUsage        { $propUsage = $pu.propUsage; }
	|	cid='OBJVALUE'	        { $propUsage = new PropertyUsage($cid.text); }
	|	cid='ADDOBJ'	        { $propUsage = new PropertyUsage($cid.text); }
	|	cid='ADDFORM'	        { $propUsage = new PropertyUsage($cid.text); }
	|	cid='ADDNESTEDFORM'	    { $propUsage = new PropertyUsage($cid.text); }
	|	cid='ADDSESSIONFORM'	{ $propUsage = new PropertyUsage($cid.text); }
	|	cid='EDITFORM'	        { $propUsage = new PropertyUsage($cid.text); }
	|	cid='EDITNESTEDFORM'	{ $propUsage = new PropertyUsage($cid.text); }
	|	cid='EDITSESSIONFORM'	{ $propUsage = new PropertyUsage($cid.text); }
	|	cid='DELETE'		    { $propUsage = new PropertyUsage($cid.text); }
	|	cid='DELETESESSION'	    { $propUsage = new PropertyUsage($cid.text); }
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
		decl=formFilterDeclaration { properties.add($decl.property); propertyMappings.add($decl.mapping);}
	    (',' decl=formFilterDeclaration { properties.add($decl.property); propertyMappings.add($decl.mapping);})*
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
	:	'ON' 
		(	'OK' 	 { $type = FormEventType.OK; }
		|	'APPLY'	 { $type = FormEventType.APPLY; }	
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
	:   'FILTERGROUP' sid=ID { filterGroupSID = $sid.text; }
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
    :   'FILTER' caption=stringLiteral keystroke=stringLiteral fd=formFilterDeclaration setDefault=filterSetDefault
        {
            $filter = new RegularFilterInfo($caption.val, $keystroke.val, $fd.property, $fd.mapping, $setDefault.isDefault);
        }
    ;
	
formFilterDeclaration returns [LP property, List<String> mapping] 
@init {
	List<TypedParameter> context = null;
	if (inPropParseState()) {
		context = $formStatement::form.getTypedObjectsNames(self.getVersion());
	}
}
@after {
	if (inPropParseState()) {
		$mapping = $formStatement::form.getUsedObjectNames(context, $expr.property.usedParams);
	}	
}
	:	expr=propertyExpression[context, false] { if (inPropParseState()) { self.checkNecessaryProperty($expr.property); $property = $expr.property.property; } }
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
	:	pDraw=formPropertyDraw { $property = $pDraw.property; } ('ASC' | 'DESC' { $order = false; })?
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
	int lineNumber = self.getParser().getCurrentParserLineNumber(); 
}
@after {
	if (inPropParseState()) {
	    if (property != null) // not native
		self.setPropertyScriptInfo(property, $text, lineNumber);
	}
}
	:	declaration=propertyDeclaration { if ($declaration.params != null) { context = $declaration.params; dynamic = false; } }
		'=' 
		(	def=expressionUnfriendlyPD[context, dynamic, false] { property = $def.property; signature = $def.signature; }
		|	expr=propertyExpression[context, dynamic] { if (inPropParseState()) { self.checkNecessaryProperty($expr.property); signature = self.getClassesFromTypedParams(context); property = $expr.property.property; } }
		|	'NATIVE' classId '(' clist=classIdList ')' { if (inPropParseState()) { signature = self.createClassSetsFromClassNames($clist.ids); }}
		)
		propertyOptions[property, $declaration.name, $declaration.caption, context, signature]
		( {!self.semicolonNeeded()}?=>  | ';')
	;

propertyDeclaration returns [String name, String caption, List<TypedParameter> params]
	:	propNameCaption=simpleNameWithCaption { $name = $propNameCaption.name; $caption = $propNameCaption.caption; }
		('(' paramList=typedParameterList ')' { $params = $paramList.params; })? 
	;
	

propertyExpression[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property]
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
		(operand=EQ_OPERAND { op = $operand.text; }
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
	|	constDef=literal { $property = new LPWithParams($constDef.property, new ArrayList<Integer>()); }
	;

expressionUnfriendlyPD[List<TypedParameter> context, boolean dynamic, boolean innerPD] returns [LP property, List<ResolveClassSet> signature]
	:	ciPD=contextIndependentPD[innerPD] { $property = $ciPD.property; $signature = $ciPD.signature; }
	|	actPD=actionPropertyDefinition[context, dynamic] { if (inPropParseState()) { $property = $actPD.property.property; $signature = $actPD.signature;  }}	
	;

contextIndependentPD[boolean innerPD] returns [LP property, List<ResolveClassSet> signature]
	: 	dataDef=dataPropertyDefinition[innerPD] { $property = $dataDef.property; $signature = $dataDef.signature; }
	|	abstractDef=abstractPropertyDefinition { $property = $abstractDef.property; $signature = $abstractDef.signature; }
	|	abstractActionDef=abstractActionPropertyDefinition { $property = $abstractActionDef.property; $signature = $abstractActionDef.signature; }
	|	formulaProp=formulaPropertyDefinition { $property = $formulaProp.property; $signature = $formulaProp.signature; }
	|	groupDef=groupPropertyDefinition { $property = $groupDef.property; $signature = $groupDef.signature; }
	|	filterProp=filterPropertyDefinition { $property = $filterProp.property; $signature = $filterProp.signature; }
	;

joinPropertyDefinition[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property]
@init {
	boolean isInline = false;
}
@after {
	if (inPropParseState()) {
		if (isInline) {
			$property = self.addScriptedJProp($iProp.property, $exprList.props);
		} else {
			$property = self.addScriptedJProp($uProp.propUsage, $exprList.props, context);	
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
		$property = self.addScriptedGProp($type.type, $mainList.props, groupProps, orderProps, ascending, $whereExpr.property);
	}
}
	:	'GROUP'
		type=groupingType
		mainList=nonEmptyPropertyExpressionList[groupContext, true]
		('BY'
		exprList=nonEmptyPropertyExpressionList[groupContext, true] { groupProps.addAll($exprList.props); })?
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
	boolean sessionProp = false;
}
@after {
	if (inPropParseState()) {
		$signature = self.createClassSetsFromClassNames($paramClassNames.ids); 
		$property = self.addScriptedDProp($returnClass.sid, $paramClassNames.ids, sessionProp, innerPD, false);
	}
}
	:	'DATA'
		('SESSION' { sessionProp = true; } )?
		returnClass=classId
		'('
			paramClassNames=classIdList
		')'
	;


abstractPropertyDefinition returns [LP property, List<ResolveClassSet> signature]
@init {
	boolean isExclusive = true;
	boolean isChecked = false;
	CaseUnionProperty.Type type = CaseUnionProperty.Type.MULTI;	
}
@after {
	if (inPropParseState()) {
		$signature = self.createClassSetsFromClassNames($paramClassNames.ids); 
		$property = self.addScriptedAbstractProp(type, $returnClass.sid, $paramClassNames.ids, isExclusive, isChecked);	
	}
}
	:	'ABSTRACT'
		(
			'CASE' { type = CaseUnionProperty.Type.CASE; isExclusive = false; } (opt=exclusiveOverrideOption { isExclusive = $opt.isExclusive; })?
		|	'MULTI'	{ type = CaseUnionProperty.Type.MULTI; isExclusive = true; } (opt=exclusiveOverrideOption { isExclusive = $opt.isExclusive; })?
		|	'OVERRIDE' { type = CaseUnionProperty.Type.VALUE; isExclusive = false; }
		|	'EXCLUSIVE'{ type = CaseUnionProperty.Type.VALUE; isExclusive = true; }	
		)?
		('CHECKED' { isChecked = true; })?
		returnClass=classId
		'('
			paramClassNames=classIdList
		')'
	;

abstractActionPropertyDefinition returns [LP property, List<ResolveClassSet> signature]
@init {
	boolean isExclusive = true;	
	boolean isChecked = false;
	ListCaseActionProperty.AbstractType type = ListCaseActionProperty.AbstractType.MULTI;
}
@after {
	if (inPropParseState()) {
		$signature = self.createClassSetsFromClassNames($paramClassNames.ids); 
		$property = self.addScriptedAbstractActionProp(type, $paramClassNames.ids, isExclusive, isChecked);
	}
}
	:	'ABSTRACT' 'ACTION' 
		(
			'CASE' (opt=exclusiveOverrideOption { type = ListCaseActionProperty.AbstractType.CASE; isExclusive = $opt.isExclusive; })?
		|	'MULTI'	(opt=exclusiveOverrideOption { isExclusive = $opt.isExclusive; })?
		|	'LIST' { type = ListCaseActionProperty.AbstractType.LIST; }
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

formulaPropertySyntaxType returns [SQLSyntaxType type = null]
    : ('PG' { $type = SQLSyntaxType.POSTGRES; } | 'MS' { $type = SQLSyntaxType.MSSQL; })? 
;
formulaPropertySyntax returns [List<SQLSyntaxType> types = new ArrayList<SQLSyntaxType>(), List<String> strings = new ArrayList<String>()]
    :
    (type=formulaPropertySyntaxType { $types.add($type.type); } formulaText=stringLiteral { $strings.add($formulaText.val); })+
;

formulaPropertyDefinition returns [LP property, List<ResolveClassSet> signature]
@init {
	String className = null;
	boolean hasNotNull = false;
}
@after {
	if (inPropParseState()) {
		$property = self.addScriptedSFProp(className, $synt.types, $synt.strings, hasNotNull);
		$signature = Collections.<ResolveClassSet>nCopies($property.listInterfaces.size(), null);
	}
}
	:	'FORMULA'
	    ('NULL' { hasNotNull = true; })?
		(clsName=classId { className = $clsName.sid; })?
		synt = formulaPropertySyntax
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
		gobj=groupObjectID
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
}
	:	'[' '='	(	expr=propertyExpression[newContext, true] { if (inPropParseState()) { self.checkNecessaryProperty($expr.property); $property = $expr.property.property; } }
			|	def=expressionUnfriendlyPD[newContext, true, true] { $property = $def.property; }
			)
		']'
	;

propertyName returns [String name] 
	:	id=compoundID { $name = $id.sid; }
	;

propertyOptions[LP property, String propertyName, String caption, List<TypedParameter> context, List<ResolveClassSet> signature]
@init {
	String groupName = null;
	String table = null;
	boolean isPersistent = false;
	boolean isComplex = false;
	Boolean isLoggable = null;
	BooleanDebug notNull = null;
	BooleanDebug notNullResolve = null;
	Event notNullEvent = null;
}
@after {
	if (inPropParseState() && property != null) { // not native
		self.addSettingsToProperty(property, propertyName, caption, context, signature, groupName, isPersistent, isComplex, table, notNull, notNullResolve, notNullEvent);	
		self.makeLoggable(property, isLoggable);
	}
}
	: 	(	'IN' name=compoundID { groupName = $name.sid; }
		|	'PERSISTENT' { isPersistent = true; }
		|	'COMPLEX' { isComplex = true; }
		|	'TABLE' tbl = compoundID { table = $tbl.sid; }
		|	shortcutSetting [property, caption != null ? caption : propertyName]
		|	asEditActionSetting [property]
		|	toolbarSetting [property]
		|	fixedCharWidthSetting [property]
		|	minCharWidthSetting [property]
		|	maxCharWidthSetting [property]
		|	prefCharWidthSetting [property]
		|	imageSetting [property]
		|	editKeySetting [property]
		|	autosetSetting [property]
		|	confirmSetting [property]
		|	regexpSetting [property]
		|	loggableSetting { isLoggable = true; }
		|	echoSymbolsSetting [property]
		|	indexSetting [property]
		|	aggPropSetting [property]
		|	s=notNullSetting { 
		    notNull = new BooleanDebug($s.debugInfo);
		    notNullResolve = $s.toResolve; 
		    notNullEvent = $s.event; 
        }
		|	onEditEventSetting [property, context]
		|	eventIdSetting [property]
		)*
	;


shortcutSetting [LP property, String caption]
@after {
	if (inPropParseState()) {
		self.addToContextMenuFor(property, caption, $usage.propUsage);
	}
}
	:	'SHORTCUT' usage = propertyUsage 
	;

asEditActionSetting [LP property]
@init {
	String editActionSID = null;
}
@after {
	if (inPropParseState()) {
		self.setAsEditActionFor(property, editActionSID, $usage.propUsage);
	}
}
	:	(   'ASONCHANGE' { editActionSID = ServerResponse.CHANGE; }
		|   'ASONCHANGEWYS' { editActionSID = ServerResponse.CHANGE_WYS; }
		|   'ASONEDIT' { editActionSID = ServerResponse.EDIT_OBJECT; }
        	)
	        usage = propertyUsage 
	;

toolbarSetting [LP property]
@after {
	if (inPropParseState()) {
		self.setDrawToToolbar(property);
	}
}
	:	'TOOLBAR'
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

loggableSetting
	:	'LOGGABLE'
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

notNullSetting returns [ActionDebugInfo debugInfo, BooleanDebug toResolve = null, Event event]
@init {
    $debugInfo = self.getEventStackDebugInfo();
}
	:	'NOT' 'NULL' 
	    (dt = notNullDeleteSetting { $toResolve = new BooleanDebug($dt.debugInfo); })? 
	    et=baseEvent { $event = $et.event; }
;

notNullDeleteSetting returns [ActionDebugInfo debugInfo]
@init {
    $debugInfo = self.getEventStackDebugInfo();
}
    :   'DELETE'
;

onEditEventSetting [LP property, List<TypedParameter> context]
@init {
	String type = null;
}
@after {
	if (inPropParseState()) {
		self.setScriptedEditAction(property, type, $action.property);
	}
}
	:	'ON'
	    (   'CHANGE' { type = ServerResponse.CHANGE; }
	    |   'CHANGEWYS' { type = ServerResponse.CHANGE_WYS; }
	    |   'EDIT' { type = ServerResponse.EDIT_OBJECT; }
	    )
		action=topActionPropertyDefinitionBody[context, false, false]
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

actionPropertyDefinition[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property, List<ResolveClassSet> signature]
@init {
	List<TypedParameter> localContext = context;
	boolean localDynamic = dynamic;
	boolean ownContext = false;
}
@after {
	if (inPropParseState()) {
		self.checkActionAllParamsUsed(localContext, $property.property, ownContext);
	}
}
	:	'ACTION'
		( '(' list=typedParameterList ')' 
			{ 
				localContext = $list.params; localDynamic = false; ownContext = true; 
				
				if (inPropParseState() && !dynamic)	{
					self.mergeActionLocalContext(context, localContext);
				}
			} 
		)?
		pdb=topActionPropertyDefinitionBody[localContext, localDynamic, true] { if (inPropParseState()) { $property = $pdb.property; $signature = $pdb.signature; }}
	;

// top level, not recursive
topActionPropertyDefinitionBody[List<TypedParameter> context, boolean dynamic, boolean needFullContext] returns [LPWithParams property, List<ResolveClassSet> signature]
    : PDB = modifyContextFlowActionPropertyDefinitionBody[new ArrayList<TypedParameter>(), context, dynamic, needFullContext] { $property = $PDB.property; $signature = $PDB.signature; }    
;

// modifies context + is flow action (uses another actions)
modifyContextFlowActionPropertyDefinitionBody[List<TypedParameter> oldContext, List<TypedParameter> newContext, boolean dynamic, boolean needFullContext] returns [LPWithParams property, List<ResolveClassSet> signature]
@after{
    if (inPropParseState()) {
        $property = self.modifyContextFlowActionPropertyDefinitionBodyCreated($property, $newContext, $oldContext, $signature, needFullContext);
    }
}
    : PDB = actionPropertyDefinitionBody[newContext, dynamic, true] { $property = $PDB.property; $signature = $PDB.signature; }    
;

innerActionPropertyDefinitionBody[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property, List<ResolveClassSet> signature]
    : PDB = actionPropertyDefinitionBody[context, dynamic, false] { $property = $PDB.property; $signature = $PDB.signature; }
;

actionPropertyDefinitionBody[List<TypedParameter> context, boolean dynamic, boolean modifyContext] returns [LPWithParams property, List<ResolveClassSet> signature]
@init {
	int line = self.getParser().getGlobalCurrentLineNumber(); 
    int offset = self.getParser().getGlobalPositionInLine();
}
@after{
    if (inPropParseState()) {
        self.actionPropertyDefinitionBodyCreated($property, line, offset, modifyContext);
    }
}
	:	extPDB=extendContextActionPDB[context, dynamic] { $property = $extPDB.property; if (inPropParseState()) $signature = self.getClassesFromTypedParams(context); }
	|	keepPDB=keepContextActionPDB[context, dynamic] 	{ $property = $keepPDB.property; if (inPropParseState()) $signature = self.getClassesFromTypedParams(context); }
	|	ciPDB=contextIndependentActionPDB	 	{ $property = $ciPDB.property; $signature = $ciPDB.signature; }
	;

extendContextActionPDB[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property]
@init { 
	if (inPropParseState() && dynamic) {
		self.getErrLog().emitExtendActionContextError(self.getParser());
	}
}
	:	setPDB=assignActionPropertyDefinitionBody[context] { $property = $setPDB.property; }
	|	forPDB=forActionPropertyDefinitionBody[context] { $property = $forPDB.property; }
	|	classPDB=changeClassActionPropertyDefinitionBody[context] { $property = $classPDB.property; }
	|	delPDB=deleteActionPropertyDefinitionBody[context] { $property = $delPDB.property; }
	|	addPDB=addObjectActionPropertyDefinitionBody[context] { $property = $addPDB.property; }
	;
	
keepContextActionPDB[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property]
	:	listPDB=listActionPropertyDefinitionBody[context, dynamic] { $property = $listPDB.property; }
	|	requestInputPDB=requestInputActionPropertyDefinitionBody[context, dynamic] { $property = $requestInputPDB.property; }
	|	execPDB=execActionPropertyDefinitionBody[context, dynamic] { $property = $execPDB.property; }	
	|	tryPDB=tryActionPropertyDefinitionBody[context, dynamic] { $property = $tryPDB.property; }
	|	ifPDB=ifActionPropertyDefinitionBody[context, dynamic] { $property = $ifPDB.property; }
	|	casePDB=caseActionPropertyDefinitionBody[context, dynamic] { $property = $casePDB.property; }
	|	multiPDB=multiActionPropertyDefinitionBody[context, dynamic] { $property = $multiPDB.property; }	
	|	termPDB=terminalFlowActionPropertyDefinitionBody { $property = $termPDB.property; }
	|   applyPDB=applyActionPropertyDefinitionBody[context, dynamic] { $property = $applyPDB.property; }
	
	|	formPDB=formActionPropertyDefinitionBody[context, dynamic] { $property = $formPDB.property; }
	|	msgPDB=messageActionPropertyDefinitionBody[context, dynamic] { $property = $msgPDB.property; }
	|	asyncPDB=asyncUpdateActionPropertyDefinitionBody[context, dynamic] { $property = $asyncPDB.property; }
	|	seekPDB=seekObjectActionPropertyDefinitionBody[context, dynamic] { $property = $seekPDB.property; }
	|	confirmPDB=confirmActionPropertyDefinitionBody[context, dynamic] { $property = $confirmPDB.property; }
	|	mailPDB=emailActionPropertyDefinitionBody[context, dynamic] { $property = $mailPDB.property; }
	|	filePDB=fileActionPropertyDefinitionBody[context, dynamic] { $property = $filePDB.property; }
	|	evalPDB=evalActionPropertyDefinitionBody[context, dynamic] { $property = $evalPDB.property; }
	|	drillDownPDB=drillDownActionPropertyDefinitionBody[context, dynamic] { $property = $drillDownPDB.property; }
	|	focusPDB=focusActionPropertyDefinitionBody[context, dynamic] { $property = $focusPDB.property; }
	;
	
contextIndependentActionPDB returns [LPWithParams property, List<ResolveClassSet> signature]
@init {
	$property = new LPWithParams(null, new ArrayList<Integer>());
}
	:	addformPDB=addFormActionPropertyDefinitionBody { $property.property = $addformPDB.property; $signature = $addformPDB.signature; }
	|	editformPDB=editFormActionPropertyDefinitionBody { $property.property = $editformPDB.property; $signature = $editformPDB.signature; }
	|	actPDB=customActionPropertyDefinitionBody { $property.property = $actPDB.property; $signature = $actPDB.signature; }
	;


formActionPropertyDefinitionBody[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property]
@init {
	FormSessionScope sessionScope = FormSessionScope.OLDSESSION;
	ModalityType modalityType = ModalityType.DOCKED;
	boolean checkOnOk = false;
	boolean showDrop = false;
	FormPrintType printType = null;
	List<String> objects = new ArrayList<String>();
	List<LPWithParams> mapping = new ArrayList<LPWithParams>();
	String contextObjectName = null;
	LPWithParams contextProperty = null;
	String initFilterPropertyName = null;
	List<String> initFilterPropertyMapping = null;
}
@after {
	if (inPropParseState()) {
		$property = self.addScriptedFAProp($formName.sid, objects, mapping, contextObjectName, contextProperty, initFilterPropertyName, initFilterPropertyMapping, modalityType, sessionScope, checkOnOk, showDrop, printType);
	}
}
	:	'FORM' formName=compoundID 
		('OBJECTS' list=formActionObjectList[context, dynamic] { objects = $list.objects; mapping = $list.exprs; })?
		('CONTEXTFILTER' objName=ID '=' contextPropertyExpr=propertyExpression[context, dynamic] { contextObjectName = $objName.text; contextProperty = $contextPropertyExpr.property; })?
		(initFilter = initFilterDefinition { initFilterPropertyMapping = $initFilter.mapping; initFilterPropertyName = $initFilter.propName; })?
		(sessScope = formSessionScopeLiteral { sessionScope = $sessScope.val; })?
		(modality = modalityTypeLiteral { modalityType = $modality.val; })?
		('CHECK' { checkOnOk = true; })?
		('SHOWDROP' { showDrop = true; })?
		(print = formPrintTypeLiteral { printType = $print.val; })?
	;

initFilterDefinition returns [String propName, List<String> mapping]
	:	'INITFILTER'
		(  pname=ID { $propName = $pname.text; }
	    	|  mappedProp=mappedPropertyDraw { $propName = $mappedProp.name; $mapping = $mappedProp.mapping; }
		)
	;

formActionObjectList[List<TypedParameter> context, boolean dynamic] returns [List<String> objects = new ArrayList<String>(), List<LPWithParams> exprs = new ArrayList<LPWithParams>()]
	:	objName=ID { $objects.add($objName.text); } '=' expr=propertyExpression[context, dynamic] { $exprs.add($expr.property); } 
		(',' objName=ID { $objects.add($objName.text); } '=' expr=propertyExpression[context, dynamic] { $exprs.add($expr.property); })*
	;
	
customActionPropertyDefinitionBody returns [LP property, List<ResolveClassSet> signature]
@after {
	if (inPropParseState()) {
		$property = self.addScriptedCustomActionProp($classN.val);	
		$signature = Collections.<ResolveClassSet>nCopies($property.listInterfaces.size(), null); 
	}
}
	:	'CUSTOM' classN = stringLiteral ('(' classIdList ')')? 
	;


addFormActionPropertyDefinitionBody returns [LP property, List<ResolveClassSet> signature]
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

editFormActionPropertyDefinitionBody returns [LP property, List<ResolveClassSet> signature]
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

addObjectActionPropertyDefinitionBody[List<TypedParameter> context] returns [LPWithParams property]
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
	:	'ADDOBJ' cid=classId
		('WHERE' pe=propertyExpression[newContext, true] { condition = $pe.property; })?
		('TO' toProp=propertyUsage '(' params=singleParameterList[newContext, false] ')' { toPropUsage = $toProp.propUsage; toPropMapping = $params.props; } )?
	;

emailActionPropertyDefinitionBody[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property]
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
}
@after {
	if (inPropParseState()) {
		$property = self.addScriptedEmailProp(fromProp, subjProp, recipTypes, recipProps, forms, formTypes, mapObjects, attachNames, attachFormats);
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

confirmActionPropertyDefinitionBody[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property]
@after {
	if (inPropParseState()) {
		$property = self.addScriptedConfirmProp($pe.property);
	}
}
	:	'CONFIRM' pe=propertyExpression[context, dynamic]
	;
		
messageActionPropertyDefinitionBody[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property]
@init {
    boolean noWait = false;
}
@after {
	if (inPropParseState()) {
		$property = self.addScriptedMessageProp($pe.property, noWait);
	}
}
	:	'MESSAGE'
	    ('NO WAIT' { noWait = true; } )?
	    pe=propertyExpression[context, dynamic]
	;

asyncUpdateActionPropertyDefinitionBody[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property]
@after {
	if (inPropParseState()) {
		$property = self.addScriptedAsyncUpdateProp($pe.property);
	}
}
	:	'ASYNCUPDATE' pe=propertyExpression[context, dynamic]
	;

seekObjectActionPropertyDefinitionBody[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property]
@after {
	if (inPropParseState()) {
		$property = self.addScriptedObjectSeekProp($gobj.sid, $pe.property);
	}
}
	:	'SEEK' gobj=groupObjectID pe=propertyExpression[context, dynamic]
	;

fileActionPropertyDefinitionBody[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property]
@init {
	boolean loadFile = false;
}
@after {
	if (inPropParseState()) {
		$property = self.addScriptedFileAProp(loadFile, $pe.property);
	}
}
	:	('LOADFILE' { loadFile = true; } | 'OPENFILE' { loadFile = false; }) 
		pe=propertyExpression[context, dynamic]	
	;

changeClassActionPropertyDefinitionBody[List<TypedParameter> context] returns [LPWithParams property]
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

deleteActionPropertyDefinitionBody[List<TypedParameter> context] returns [LPWithParams property]
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

evalActionPropertyDefinitionBody[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property]
@after {
	if (inPropParseState()) {
		$property = self.addScriptedEvalActionProp($expr.property);
	}
}
	:	'EVAL' expr=propertyExpression[context, dynamic]
	;
	
drillDownActionPropertyDefinitionBody[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property]
@after {
	if (inPropParseState()) {
		$property = self.addScriptedDrillDownActionProp($expr.property);
	}
}
	:	'DRILLDOWN' expr=propertyExpression[context, dynamic]
	;	

focusActionPropertyDefinitionBody[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property]
@init {
    FormEntity form = null;
}
@after {
	if (inPropParseState()) {
		$property = self.addScriptedFocusActionProp($prop.propertyDraw);
	}
}
	:	'FOCUS'
	    (namespacePart=ID '.')? formPart=ID '.'
	    {   
	        if (inPropParseState()) {
	            form = self.findForm(($namespacePart != null ? $namespacePart.text + '.' : "") + $formPart.text);
            }
	    }
	    prop=formPropertySelector[form]
	;	

requestInputActionPropertyDefinitionBody[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property]
@after {
	if (inPropParseState()) {
		$property = self.addScriptedRequestUserInputAProp($tid.sid, $objID.text, $PDB.property);
	}
}
	:	'REQUEST' tid=typeId
		(	'INPUT'
		|	(objID=ID)? PDB=innerActionPropertyDefinitionBody[context, dynamic]
		)
	;

listActionPropertyDefinitionBody[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property]
@init {
	List<LPWithParams> props = new ArrayList<LPWithParams>();
	List<LP> localProps = new ArrayList<LP>();
	boolean newSession = false;
	List<PropertyUsage> migrateSessionProps = Collections.emptyList();
	boolean migrateAllSessionProps = false;
	boolean isNested = false;
	boolean doApply = false;
	boolean singleApply = false;
	boolean newThread = false;
	long ldelay = 0;
	Long lperiod = null;
}
@after {
	if (inPropParseState()) {
		$property = self.addScriptedListAProp(newSession, migrateSessionProps, migrateAllSessionProps, isNested, doApply, singleApply, props, localProps, newThread, ldelay, lperiod);
	}
}
	:   (
            (
                (   'NEWSESSION'
                    (   mps=migratePropertiesSelector { migrateAllSessionProps = $mps.all; migrateSessionProps = $mps.props; }
                    |   'NESTED' { isNested = true; }
                    )?
                )
                |
                ('NEWTHREAD' { newThread = true; } (period=intLiteral { lperiod = (long)$period.val; })? ('DELAY' delay=intLiteral { ldelay = $delay.val; })? ) 
            ) { newSession = true; }
            ('AUTOAPPLY' {doApply = true; } )?
            ('SINGLE' { singleApply = true; })? 
        )?
		'{'
			(	(PDB=innerActionPropertyDefinitionBody[context, dynamic] { props.add($PDB.property); }
				( {!self.semicolonNeeded()}?=>  | ';'))
			|	def=localDataPropertyDefinition ';' { localProps.add($def.property); }
			|	emptyStatement
			)*
		'}'
	;

migratePropertiesSelector returns[boolean all = false, List<PropertyUsage> props = new ArrayList<PropertyUsage>()]
    :   '('
            (   MULT { $all = true; }
            |   list=nonEmptyPropertyUsageList { $props = $list.propUsages; }
            )
        ')'
    ;
	
localDataPropertyDefinition returns [LP property]
@after {
	if (inPropParseState()) {
		$property = self.addLocalDataProperty($propName.text, $returnClass.sid, $paramClasses.ids);
	}
}
	:	'LOCAL' propName=ID 
		'=' returnClass=classId
		'('
			paramClasses=classIdList
		')'
	;

execActionPropertyDefinitionBody[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property]
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

assignActionPropertyDefinitionBody[List<TypedParameter> context] returns [LPWithParams property]
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

tryActionPropertyDefinitionBody[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property]
@after {
	if (inPropParseState()) {
		$property = self.addScriptedTryAProp($tryPDB.property, $finallyPDB.property);
	}
}
	:	'TRY' tryPDB=innerActionPropertyDefinitionBody[context, dynamic] 
		('FINALLY' finallyPDB=innerActionPropertyDefinitionBody[context, dynamic])?
	;

ifActionPropertyDefinitionBody[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property]
@after {
	if (inPropParseState()) {
		$property = self.addScriptedIfAProp($expr.property, $thenPDB.property, $elsePDB.property);
	}
}
	:	'IF' expr=propertyExpression[context, dynamic] 
		'THEN' thenPDB=innerActionPropertyDefinitionBody[context, dynamic]
		('ELSE' elsePDB=innerActionPropertyDefinitionBody[context, dynamic])?
	;

caseActionPropertyDefinitionBody[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property] 
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
			('ELSE' elseAct=innerActionPropertyDefinitionBody[context, dynamic] { elseAction = $elseAct.property; })?
	;

actionCaseBranchBody[List<TypedParameter> context, boolean dynamic] returns [LPWithParams whenProperty, LPWithParams thenAction]
	:	'WHEN' whenExpr=propertyExpression[context, dynamic] { $whenProperty = $whenExpr.property; }
		'THEN' thenAct=innerActionPropertyDefinitionBody[context, dynamic] { $thenAction = $thenAct.property; }
	;

applyActionPropertyDefinitionBody[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property]
@init {
	boolean single = false;
	List<PropertyUsage> keepSessionProps = Collections.emptyList();
	boolean keepAllSessionProps = false;
}
@after {
	if (inPropParseState()) {
		$property = self.addScriptedApplyAProp($applyPDB.property, single, keepSessionProps, keepAllSessionProps);
	}
}
	:	'APPLY' 
        (mps=migratePropertiesSelector { keepAllSessionProps = $mps.all; keepSessionProps = $mps.props; })?
        ('SINGLE' { single = true; })?
        applyPDB=innerActionPropertyDefinitionBody[context, dynamic]
	;

multiActionPropertyDefinitionBody[List<TypedParameter> context, boolean dynamic] returns [LPWithParams property] 
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

forAddObjClause[List<TypedParameter> context] returns [Integer paramCnt, String className]
@init {
	String varName = "added";
}
@after {
	if (inPropParseState()) {
		$paramCnt = self.getParamIndex(self.new TypedParameter($className, varName), context, true, insideRecursion);
	}
}
	:	'ADDOBJ'
		(varID=ID '=' {varName = $varID.text;})?
		addClass=classId { $className = $addClass.sid; }
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
		$property = self.addScriptedForAProp(context, $expr.property, orders, $actPDB.property, $elsePDB.property, $addObj.paramCnt, $addObj.className, recursive, descending, $in.noInline, $in.forceInline, newContext);
	}	
}
	:	(	'FOR' 
		| 	'WHILE' { recursive = true; }
		)
		(expr=propertyExpression[newContext, true]
		('ORDER'
			('DESC' { descending = true; } )? 
			ordExprs=nonEmptyPropertyExpressionList[newContext, false] { orders = $ordExprs.props; }
		)?)?
		in = inlineStatement[newContext]
		(addObj=forAddObjClause[newContext])?
		'DO' actPDB=modifyContextFlowActionPropertyDefinitionBody[context, newContext, false, false]
		( {!recursive}?=> 'ELSE' elsePDB=innerActionPropertyDefinitionBody[context, false])?
	;

terminalFlowActionPropertyDefinitionBody returns [LPWithParams property]
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
		(	expr=propertyExpression[context, dynamic] { property = $expr.property; }
		|	action=actionPropertyDefinition[context, dynamic] { property = $action.property; }
		)
		( {!self.semicolonNeeded()}?=>  | ';')
	;

////////////////////////////////////////////////////////////////////////////////
//////////////////////////////// CONSTRAINT STATEMENT //////////////////////////
////////////////////////////////////////////////////////////////////////////////

constraintStatement 
@init {
	boolean checked = false;
	List<PropertyUsage> propUsages = null;
	ActionDebugInfo debugInfo = self.getEventStackDebugInfo();
}
@after {
	if (inPropParseState()) {
		self.addScriptedConstraint($expr.property.property, $et.event, checked, propUsages, $message.val, debugInfo);
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
		'MESSAGE' message=stringLiteral
		';'
	;


////////////////////////////////////////////////////////////////////////////////
///////////////////////////////// FOLLOWS STATEMENT ////////////////////////////
////////////////////////////////////////////////////////////////////////////////

followsStatement
@init {
	List<TypedParameter> context;
	PropertyUsage mainProp;
	List<LPWithParams> props = new ArrayList<LPWithParams>();
	List<List<PropertyFollowsDebug>> options = new ArrayList<List<PropertyFollowsDebug>>();
	List<Event> events = new ArrayList<Event>();
	Event event = Event.APPLY;
	List<ActionDebugInfo> debugInfos = new ArrayList<ActionDebugInfo>(); 
}
@after {
	if (inPropParseState()) {
		self.addScriptedFollows(mainProp, context, options, props, events, debugInfos);
	}
}
	:	prop=mappedProperty { mainProp = $prop.propUsage; context = $prop.mapping; }
		'=>'
		fcl=followsClause[context] {
            props.add($fcl.prop); 
            options.add($fcl.pfollows);
            events.add($fcl.event);
            debugInfos.add($fcl.debug);
		}		
		(','
    		nfcl=followsClause[context] {
                props.add($nfcl.prop); 
                options.add($nfcl.pfollows);
                events.add($nfcl.event);
                debugInfos.add($nfcl.debug);
			}
		)*
		';'
;
	
followsClause[List<TypedParameter> context] returns [LPWithParams prop, Event event = Event.APPLY, ActionDebugInfo debug, List<PropertyFollowsDebug> pfollows = new ArrayList<PropertyFollowsDebug>()] 
@init {
    $debug = self.getEventStackDebugInfo();
}
    :   expr = propertyExpression[context, false] 
        ('RESOLVE' (ct = followsClauseType {$pfollows.add($ct.type); } )+ 
                   et=baseEvent { $event = $et.event; } 
        )? { $prop = $expr.property; }
;

followsClauseType returns [PropertyFollowsDebug type]
@init {
	ActionDebugInfo debugInfo = self.getEventStackDebugInfo(); 
}
    :	lit=LOGICAL_LITERAL	
        { 
            $type = new PropertyFollowsDebug($lit.text.equals("TRUE"), debugInfo); 
        }
;

followsResolveType returns [Integer type]
	:	lit=LOGICAL_LITERAL	{ $type = $lit.text.equals("TRUE") ? PropertyFollows.RESOLVE_TRUE : PropertyFollows.RESOLVE_FALSE; }
	|	'ALL'			{ $type = PropertyFollows.RESOLVE_ALL; }
	|	'NOTHING'		{ $type = PropertyFollows.RESOLVE_NOTHING; }
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
		valueExpr=propertyExpression[$mainProp.mapping, false] 
		'WHEN'
		('DO' { action = true; })?
		whenExpr=propertyExpression[$mainProp.mapping, false]
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
	
    ActionDebugInfo debug = self.getEventStackDebugInfo(); 
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
		in=inlineStatement[context]
		'DO'
		action=topActionPropertyDefinitionBody[context, false, false]
		(	'ORDER' ('DESC' { descending = true; })?
			orderList=nonEmptyPropertyExpressionList[context, false] { orderProps.addAll($orderList.props); }
		)?
		( {!self.semicolonNeeded()}?=>  | ';')
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
		action=topActionPropertyDefinitionBody[new ArrayList<TypedParameter>(), false, false]
		{
			if (inPropParseState()) {
				self.dropPrevScope($et.event);
			}
		}
		( {!self.semicolonNeeded()}?=>  | ';')
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
	:	('GLOBAL' { baseEvent = SystemEvent.APPLY; } | 'SESSION'	{ baseEvent = SystemEvent.SESSION; })?
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
		mainProp=mappedProperty 'DO' action=topActionPropertyDefinitionBody[$mainProp.mapping, false, false]
		( {!self.semicolonNeeded()}?=>  | ';')
	;


////////////////////////////////////////////////////////////////////////////////
////////////////////////////////// TABLE STATEMENT /////////////////////////////
////////////////////////////////////////////////////////////////////////////////

tableStatement 
@after {
	if (inTableParseState()) {
		self.addScriptedTable($name.text, $list.ids);
	}
}
	:	'TABLE' name=ID '(' list=nonEmptyClassIdList ')' ';';

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

indexStatement
@after {
	if (inIndexParseState()) {
		self.addScriptedIndex($list.propUsages);
	}	
}
	:	'INDEX' list=nonEmptyPropertyUsageList ';'
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
		self.addScriptedWindow($type.type, $id.text, $caption.val, $opts.options);
	}
}
	:	'WINDOW' type=windowType id=ID caption=stringLiteral opts=windowOptions ';'
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
			|	setupNavigatorElementStatement
			|	emptyStatement
			)*
		'}'
	| emptyStatement
	;

addNavigatorElementStatement[NavigatorElement parentElement]
	:	'ADD' elem=navigatorElementSelector (caption=stringLiteral)? posSelector=navigatorElementInsertPositionSelector[parentElement] ('TO' wid=compoundID)?
		{
			if (inPropParseState()) {
				self.setupNavigatorElement($elem.element, $caption.val, $posSelector.parent, $posSelector.position, $posSelector.anchor, $wid.sid);
			}
		}
		navigatorElementStatementBody[$elem.element]
	;

newNavigatorElementStatement[NavigatorElement parentElement]
@init {
	NavigatorElement newElement = null;
}
	:	'NEW' id=ID ('ACTION' au=propertyUsage)? caption=stringLiteral posSelector=navigatorElementInsertPositionSelector[parentElement] ('TO' wid=compoundID)? ('IMAGE' path=stringLiteral)?
		{
			if (inPropParseState()) {
				newElement = self.createScriptedNavigatorElement($id.text, $caption.val, $posSelector.parent, $posSelector.position, $posSelector.anchor, $wid.sid, $au.propUsage, $path.val);
			}
		}
		navigatorElementStatementBody[newElement]
	;
	
navigatorElementInsertPositionSelector[NavigatorElement parentElement] returns [NavigatorElement parent, InsertPosition position, NavigatorElement anchor]
@init {
	$parent = parentElement;
	$position = InsertPosition.IN;
	$anchor = null;
}
	:	(	'IN' { $position = InsertPosition.IN; }
			elem=navigatorElementSelector { $parent = $elem.element; }
		)?
		(
			(pos=insertRelativePositionLiteral { $position = $pos.val; }
			elem=navigatorElementSelector { $anchor = $elem.element; })
		|	'FIRST' { $position = InsertPosition.FIRST; }
		)?
	;

setupNavigatorElementStatement
	:	elem=navigatorElementSelector (caption=stringLiteral)? ('TO' wid=compoundID)?
		{
			if (inPropParseState()) {
				self.setupNavigatorElement($elem.element, $caption.val, null, null, null, $wid.sid);
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
	:	(	decl=designDeclaration		{ $designStatement::design = formView = $decl.view; }
		|	edecl=extendDesignDeclaration	{ $designStatement::design = formView = $edecl.view; }	
		)
		componentStatementBody[formView == null ? null : formView.getView(), formView == null ? null : formView.getMainContainer()]
	;

designDeclaration returns [ScriptingFormView view]
@init {
	boolean applyDefault = false;
}
@after {
	if (inPropParseState()) {
		$view = self.createScriptedFormView($cid.sid, $caption.val, applyDefault);
	}
}
	:	'DESIGN' cid=compoundID (caption=stringLiteral)? ('FROM' 'DEFAULT' { applyDefault = true; })?
	;

extendDesignDeclaration returns [ScriptingFormView view]
@after {
	if (inPropParseState()) {
		$view = self.getDesignForExtending($cid.sid);
	}
}
	:	'EXTEND' 'DESIGN' cid=compoundID 
	;	

componentStatementBody [Object propertyReceiver, ComponentView parentComponent]
	:	'{'
			(	setObjectPropertyStatement[propertyReceiver]
			|	setupComponentStatement
			|	setupGroupObjectStatement
			|	newComponentStatement[parentComponent]
			|	addComponentStatement[parentComponent]
			|	removeComponentStatement
			|	emptyStatement
			)*
		'}'
	|	emptyStatement
	;

setupComponentStatement
	:	comp=componentSelector componentStatementBody[$comp.component, $comp.component]
	;

setupGroupObjectStatement
@init {
	GroupObjectView groupObject = null;
}
	:	'GROUP'
		'('
			ID
			{
				if (inPropParseState()) {
					groupObject = $designStatement::design.getGroupObject($ID.text, self.getVersion());
				}
			}
		')'
		'{'
			( setObjectPropertyStatement[groupObject]
			| emptyStatement
			)*
		'}'
	;

newComponentStatement[ComponentView parentComponent]
@init {
	ComponentView newComp = null;
}
	:	'NEW' cid=multiCompoundID insPosition=componentInsertPositionSelector[parentComponent]
		{
			if (inPropParseState()) {
				newComp = $designStatement::design.createNewComponent($cid.sid, insPosition.parent, insPosition.position, insPosition.anchor, self.getVersion());
			}
		}
		componentStatementBody[newComp, newComp]
	;
	
addComponentStatement[ComponentView parentComponent]
@init {
	ComponentView insComp = null;
}
	:	'ADD' insSelector=componentSelector { insComp = $insSelector.component; } insPosition=componentInsertPositionSelector[parentComponent]
		{
			if (inPropParseState()) {
				$designStatement::design.moveComponent(insComp, insPosition.parent, insPosition.position, insPosition.anchor, self.getVersion());
			}
		}
		componentStatementBody[insComp, insComp]
	;
	
componentInsertPositionSelector[ComponentView parentComponent] returns [ComponentView parent, InsertPosition position, ComponentView anchor]
@init {
	$parent = parentComponent;
	$position = InsertPosition.IN;
	$anchor = null;
}
	:	(	'IN' { $position = InsertPosition.IN; }
			comp=componentSelector { $parent = $comp.component; }
		)?
		(
			(pos=insertRelativePositionLiteral { $position = $pos.val; }
			comp=componentSelector { $anchor = $comp.component; })
		|	'FIRST' { $position = InsertPosition.FIRST; }
		)?
	;

removeComponentStatement
@init {
	boolean cascade = false;
}
	:	'REMOVE' compSelector=componentSelector ('CASCADE' { cascade = true; } )? ';'
		{
			if (inPropParseState()) {
				$designStatement::design.removeComponent($compSelector.component, cascade, self.getVersion());
			}
		}
	;

componentSelector returns [ComponentView component]
	:	'PARENT' '(' child=componentSelector ')'
		{
			if (inPropParseState()) {
				$designStatement::design.getParentContainer($child.component, self.getVersion());
			}
		}
	|	'PROPERTY' '(' prop=propertySelector ')' { $component = $prop.propertyView; }
	|	mid=multiCompoundID
		{
			if (inPropParseState()) {
				$component = $designStatement::design.getComponentBySID($mid.sid);
			}
		}
	;


propertySelector returns [PropertyDrawView propertyView = null]
	:	pname=ID
		{
			if (inPropParseState()) {
				$propertyView = $designStatement::design.getPropertyView($pname.text, self.getVersion());
			}
		}
	|	mappedProp=mappedPropertyDraw	
		{
			if (inPropParseState()) {
				$propertyView = $designStatement::design.getPropertyView($mappedProp.name, $mappedProp.mapping, self.getVersion());
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
    int positionInLine = self.getParser().getCurrentParserPositionInLine();
	ScriptParser.State oldState = null; 
}
@after {
	self.runMetaCode($id.sid, $list.ids, lineNumber, positionInLine);
}
	:	'@' id=compoundID '(' list=metaCodeIdList ')' 
		('{' 	
		{ 	if (self.getParser().enterGeneratedMetaState()) {  
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
	:	ID | NUMBERED_PARAM | RECURSIVE_PARAM
	;

typedParameter returns [TypedParameter param]
@after {
	if (inPropParseState()) {
		$param = self.new TypedParameter($cname.sid, $pname.text);
	}
}
	:	(cname=classId)? pname=ID
	;

simpleNameWithCaption returns [String name, String caption] 
	:	simpleName=ID { $name = $simpleName.text; }
		(captionStr=stringLiteral { $caption = $captionStr.val; })?
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
	:	first=innerActionPropertyDefinitionBody[context, dynamic] { $props.add($first.property); }
		(',' next=innerActionPropertyDefinitionBody[context, dynamic] { $props.add($next.property); })* 
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

literal returns [LP property]
@init {
	ScriptingLogicsModule.ConstType cls = null;
	Object value = null;
}
@after {
	if (inPropParseState()) {
		$property = self.addConstantProp(cls, value);	
	}
}
	: 	vint=uintLiteral	{ cls = ScriptingLogicsModule.ConstType.INT; value = $vint.val; }
	|	vlong=ulongLiteral	{ cls = ScriptingLogicsModule.ConstType.LONG; value = $vlong.val; }
	|	vnum=UNUMERIC_LITERAL	{ cls = ScriptingLogicsModule.ConstType.NUMERIC; value = $vnum.text; }
	|	vdouble=udoubleLiteral { cls = ScriptingLogicsModule.ConstType.REAL; value = $vdouble.val; }
	|	vstr=stringLiteral	{ cls = ScriptingLogicsModule.ConstType.STRING; value = $vstr.val; }  
	|	vbool=booleanLiteral	{ cls = ScriptingLogicsModule.ConstType.LOGICAL; value = $vbool.val; }
	|	vdate=dateLiteral	{ cls = ScriptingLogicsModule.ConstType.DATE; value = $vdate.val; }
	|	vdatetime=dateTimeLiteral { cls = ScriptingLogicsModule.ConstType.DATETIME; value = $vdatetime.val; }
	|	vtime=timeLiteral 	{ cls = ScriptingLogicsModule.ConstType.TIME; value = $vtime.val; }
	|	vsobj=staticObjectID { cls = ScriptingLogicsModule.ConstType.STATIC; value = $vsobj.sid; }
	|	vnull=NULL_LITERAL 	{ cls = ScriptingLogicsModule.ConstType.NULL; }
	|	vcolor=colorLiteral { cls = ScriptingLogicsModule.ConstType.COLOR; value = $vcolor.val; }		
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

groupObjectID returns [String sid]
    :	(namespacePart=ID '.')? formPart=ID '.' namePart=ID { $sid = ($namespacePart != null ? $namespacePart.text + '.' : "") + $formPart.text + '.' + $namePart.text; }
    ;

multiCompoundID returns [String sid]
	:	id=ID { $sid = $id.text; } ('.' cid=ID { $sid = $sid + '.' + $cid.text; } )*
	;

exclusiveOverrideOption returns [boolean isExclusive]
	:	'OVERRIDE' { $isExclusive = false; }
	|	'EXCLUSIVE'{ $isExclusive = true; } 
	;

colorLiteral returns [Color val]
	:	c=COLOR_LITERAL { $val = Color.decode($c.text); }
	|	'RGB' '(' r=uintLiteral ',' g=uintLiteral ',' b=uintLiteral ')' { $val = self.createScriptedColor($r.val, $g.val, $b.val); } 
	;

stringLiteral returns [String val]
	:	s=STRING_LITERAL { $val = self.transformStringLiteral($s.text); }
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

modalityTypeLiteral returns [ModalityType val]
	:	'DOCKED' { $val = ModalityType.DOCKED; }
	|	'MODAL' { $val = ModalityType.MODAL; }
	|	'DOCKEDMODAL' { $val = ModalityType.DOCKED_MODAL; }
	|	'FULLSCREEN' { $val = ModalityType.FULLSCREEN_MODAL; }
	|	'DIALOG' { $val = ModalityType.DIALOG_MODAL; }
	;
	
formPrintTypeLiteral returns [FormPrintType val]
@init {
	$val = FormPrintType.PRINT;
} 
	:	'PRINT'
		(
			'AUTO' { $val = FormPrintType.AUTO; }
		|	'XLS' { $val = FormPrintType.XLS; }
		|	'PDF' { $val = FormPrintType.PDF; }
		)?
	;

formSessionScopeLiteral returns [FormSessionScope val]
	:	'OLDSESSION' { $val = FormSessionScope.OLDSESSION; }
	|	'NEWSESSION' { $val = FormSessionScope.NEWSESSION; }
	|	'NESTEDSESSION' { $val = FormSessionScope.NESTEDSESSION; }
	|	'MANAGESESSION' { $val = FormSessionScope.MANAGESESSION; }
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
    : MULT | DIV
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

PRIMITIVE_TYPE  :	'INTEGER' | 'DOUBLE' | 'LONG' | 'BOOLEAN' | 'DATE' | 'DATETIME' | 'YEAR' | 'TEXT'  | 'RICHTEXT' | 'TIME' | 'WORDFILE' | 'IMAGEFILE' | 'PDFFILE' | 'CUSTOMFILE' | 'EXCELFILE' | 'STRING[' DIGITS ']' | 'ISTRING[' DIGITS ']'  | 'VARSTRING[' DIGITS ']' | 'VARISTRING[' DIGITS ']' | 'NUMERIC[' DIGITS ',' DIGITS ']' | 'COLOR';
LOGICAL_LITERAL :	'TRUE' | 'FALSE';
NULL_LITERAL	:	'NULL';	
ID          	:	FIRST_ID_LETTER NEXT_ID_LETTER*;
WS		:	(NEWLINE | SPACE) { $channel=HIDDEN; };
STRING_LITERAL	:	'\'' STR_LITERAL_CHAR* '\'';
COLOR_LITERAL 	:	'#' HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT;
COMMENTS	:	('//' .* '\n') { $channel=HIDDEN; };
UINT_LITERAL 	:	DIGITS;
ULONG_LITERAL	:	DIGITS('l'|'L');
UDOUBLE_LITERAL	:	DIGITS '.' EDIGITS('d'|'D');
UNUMERIC_LITERAL: 	DIGITS '.' EDIGITS;	  
DATE_LITERAL	:	DIGIT DIGIT DIGIT DIGIT '_' DIGIT DIGIT '_' DIGIT DIGIT; 
DATETIME_LITERAL:	DIGIT DIGIT DIGIT DIGIT '_' DIGIT DIGIT '_' DIGIT DIGIT '_' DIGIT DIGIT ':' DIGIT DIGIT;	
TIME_LITERAL	:	DIGIT DIGIT ':' DIGIT DIGIT;
NUMBERED_PARAM	:	'$' DIGITS;
RECURSIVE_PARAM :	'$' FIRST_ID_LETTER NEXT_ID_LETTER*;	
EQ_OPERAND	:	('==') | ('!=');
LESS_OPERAND	: 	('<');
GR_OPERAND	:	('>');
RELEQ_OPERAND	: 	('<=') | ('>=');
MINUS       :	'-';
PLUS        :	'+';
MULT        :	'*';
DIV         :	'/';
ADDOR_OPERAND	:	'(+)' | {ahead("(-)")}?=> '(-)';
CONCAT_OPERAND	:	'##';
CONCAT_CAPITALIZE_OPERAND	:	'###';	