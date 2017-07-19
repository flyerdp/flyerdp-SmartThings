/**
 *  Smart Switch Control Based on Motion with Retrigger Timeout and No Motion Auto Off
 *
 *  This Smart App will turn on/off a switch based on Motion with retrigger 
 *  safety and turn off option when nomotion is detected after timeout.
 *  I wrote this because my non local motion detectors were turning on lights
 *  right after switching off manually because of motion detection delay to cloud
 *  and their short re-arm timeout.
 *  Copyright 2017 David Poprik
 *  GNU General Public License v2 (https://www.gnu.org/licenses/gpl-2.0.txt)
 *
 */

definition(
	name: "Motion Based Switch Control with Retrigger Safety Child",
	namespace: "flyerdp",
	author: "flyerdp@gmail.com",
	description: "Control switch(es) based on motion detection.  Includes off timer on no motion and retrigger safety timeout.",
	category: "Convenience",
	iconUrl: "https://s3.us-east-2.amazonaws.com/mysmartthings/MotionSwitchController_60x60.png",
	iconX2Url: "https://s3.us-east-2.amazonaws.com/mysmartthings/MotionSwitchController_120x120.png",
	iconX3Url: "https://s3.us-east-2.amazonaws.com/mysmartthings/MotionSwitchController_120x120.png"
)

preferences{
	page(name: "Settings")
	page(name: "OptionalSettings")
	page(name: "TimeRestrictions")
	page(name: "SetTimeRestrictions")
}

def Settings() {
	dynamicPage(name: "Settings", title: "Motion Based Trigger Rule Settings:", install: false, uninstall: true, nextPage: "OptionalSettings") {
	section("Motion Sensors and Switches To Use:")
	{
		input ("MotionSensors"
			, "capability.motionSensor"
			, title: "Select Trigger Motion Sensor(s):"
			, required: true
			, multiple:true
		)
		
		input ("ControlSwitches"
			, "capability.switch"
			, title: "Select Switch(es) To Control with Motion Sensor(s):"
			, required: true
			, multiple:true
		)
		
		paragraph "Set the amount of time to wait when switch is manually turned off before allowing new motion to retrigger."
		input ("RetriggerSafetyInterval"
			, "number"
			, title: "Motion Sensor Retrigger Safety in Seconds:"
			, required: true
			, defaultValue: 16
		)
		input ("RetriggerSafetyAppliesTo"
			, "enum"
			, title: "Retrigger Safety Applies To:"
			, required: true
			, defaultValue: "manual"
			,options: ["manual":"When Switch(es) are Manually Turned Off","auto":"When Switch(es) are Auto or Manually Turned Off"]
		)
	}
}
}

def OptionalSettings() {
	dynamicPage(name: "OptionalSettings", title: "Auto Off Options:", install: false, uninstall: true, nextPage: "TimeRestrictions") {
		section("") {
			paragraph "How many seconds or minutes of inactivity until the switch is turned off?"
			input ("AutoOffMinutes"
				, "number"
				, title: "Auto Turn Off Time (minutes)?"
				, required: false
				,submitOnChange:true
			)
			if (AutoOffMinutes > 0){
				paragraph"Turn off via inactivity timer when switched on for any reason?"
				input ("AutoOffCondition"
					,"enum"
					,title: "Always Auto Turn Off If..."
					,options: [[1:"Turned On By This Smart App"],[2:"Turned On By This Smart App or Physically"],[3:"Turned on by Any Smart App"],[4:"Turned on Physically"],[5:"Turned on by Any Smart App or Physically"]]
					,defaultValue: 1
					,required: false
				)
			}
			input ("debugEnabled"
				, "enum"
				, title: "Enable Debug Logging?"
				,options: ["False","True"]
				,defaultValue: "False"
				,required: false
				,submitOnChange:true
			)
		}
		def ishidden = true
		if (allowCustomName == "True") {ishidden = false}
		section("Custom Rule Naming", hideable: true, hidden: ishidden) {
			input ("allowCustomName"
				, "enum"
				, title: "Use a Custom Rule Name?"
				,options: ["False","True"]
				,defaultValue: "False"
				,required: false
				,submitOnChange:true
			)
			if (allowCustomName == "True"){
				input ("CustomName"
				, "text"
				, title: "Assign a Name:"
				,required: true
			)
			}
		}
	}
}

def TimeRestrictions() {
	dynamicPage(name: "TimeRestrictions", title: "Time Restrictions:", install: true, uninstall: true) {
		def TimeDescription = "Tap to Set"
		def TimePlaceholder = ""
		def schedStartTime = ""
		def schedStopTime = ""
		def TimeofDaySet = false
		def TimeofDayValue = ""

		//Validate variable settings and set global states
		if (StartAt == "Specific Time"){
			state.SpecificStartTime = SpecificStartTime
			state.StartAt = ""
		}else{
			state.SpecificStartTime = null
		}

		if (EndAt == "Specific Time"){
			state.SpecificEndTime = SpecificEndTime
			state.EndAt = ""
		}else{
			state.SpecificEndTime = null
		}
		
		if (StartAtOffSet){
			state.StartAtOffSet = StartAtOffSet
		}else{
			state.StartAtOffSet = null	
		}

		if (EndAtOffSet){
			state.EndAtOffSet = EndAtOffSet
		}else{
			state.EndAtOffSet = null
		}
		
		if (StartAt == "Sunrise" || StartAt == "Sunset"){
			state.StartAt = StartAt
			schedStartTime = "${StartAt} "
			if (state.StartAtOffSet) {
				if (state.StartAtOffSet > 0) {
					schedStartTime = schedStartTime + "+"
				}
				schedStartTime = schedStartTime + "${state.StartAtOffSet}"
			}else {state.StartAtOffSet = null}
		}else{
			state.StartAt = null
		}
		
		if (EndAt == "Sunrise" || EndAt == "Sunset"){
			state.EndAt = EndAt
			schedStopTime = "${EndAt} "
			if (state.EndAtOffSet) {
				if (state.EndAtOffSet > 0) {
					schedStopTime = schedStopTime + "+"
				}
				schedStopTime = schedStopTime + "${state.EndAtOffSet}"
			}else {state.EndAtOffSet = null}
		}else{
			state.EndAt = null
		}
		
		
		//If Specific Time Restrictions set then come up with the value to display for what is set
		if (state.SpecificStartTime){
			schedStartTime = new Date(timeToday(state.SpecificStartTime).time).format("h:mm a", location.timeZone)
		}
		if (state.SpecificEndTime){
			schedStopTime = new Date(timeToday(state.SpecificEndTime).time).format("h:mm a", location.timeZone)
		}
		
		if (schedStartTime) {
			TimeDescription = "${schedStartTime} to ${schedStopTime}"
			TimePlaceholder = TimeDescription
			TimeofDaySet = true
		}
		
		//Output Debug Variable Info if Debug Set
		if (state.debug){
			debugLog("Time of Day Set to: ${TimeDescription}")
			debugLog("Time of Day enabled: ${TimeofDaySet}")
			debugLog("StartAt Set to: ${state.StartAt}")
			debugLog("EndAt Set to: ${state.EndAt}")
			debugLog("Start Offset Set to: ${state.StartAtOffSet}")
			debugLog("EndAt Offset Set to: ${state.EndAtOffSet}")
			debugLog("Specific Start Time Set to: ${state.SpecificStartTime}")
			debugLog("Specific End Time Set to: ${state.SpecificEndTime}")
		}
		
		section("Time of Day:") {
			href(name: "href"
			,title: "Only during a certain time"
			,required: TimeofDaySet
			,description: "${TimeDescription}"
			,value: "${TimePlaceholder}"
			,page: "SetTimeRestrictions"
			)
		}

		section("On Which Days:") {
			input ("allowedDays"
				,"enum"
				,title: "Only on certain days of the week:"
				,required: false
				,multiple: true
				,options: ["Monday": "Monday", "Tuesday": "Tuesday", "Wednesday": "Wednesday", "Thursday": "Thursday", "Friday": "Friday"]
			)	
		}
	}
}

def SetTimeRestrictions() {
	def StartRequired = false
	def EndRequired = false

	if(StartAt){
		EndRequired = true
	}
	if(EndAt){
		StartRequired = true
	}
	
	dynamicPage(name: "SetTimeRestrictions", title: "Only during a certain time:"){
		def StartAtOptions = ["Sunrise":"Sunrise","Sunset":"Sunset","Specific Time":"Specific Time"]
		def EndAtOptions = ["Sunrise":"Sunrise","Sunset":"Sunset","Specific Time":"Specific Time"]

		if (StartAt == "Sunrise"){
			EndAtOptions = ["Sunset":"Sunset","Specific Time":"Specific Time"]
		}else if (StartAt == "Sunset"){
			EndAtOptions = ["Sunrise":"Sunrise","Specific Time":"Specific Time"]
		}

		if (EndAt == "Sunrise"){
			StartAtOptions = ["Sunset":"Sunset","Specific Time":"Specific Time"]
		}else if (EndAt == "Sunset"){
			StartAtOptions = ["Sunrise":"Sunrise","Specific Time":"Specific Time"]
		}
		
		section("") {
			input ("StartAt"
				,"enum"
				,title: "Starting At:"
				,options: StartAtOptions
				,required: StartRequired
				,submitOnChange: true
			)

			if (StartAt == "Sunrise" || StartAt == "Sunset") {
				input ("StartAtOffSet"
					,"number"
					,title: "Offset in minutes (+/-)"
					,required: false
					,submitOnChange: true
				)
			}else if (StartAt == "Specific Time"){
				input (name: "SpecificStartTime"
				, type: "time"
				, title: "Enter Start Time:"
				, description: null
				, required: true
				, defaultValue: null
				, submitOnChange: true
				)
			}
			input ("EndAt"
				,"enum"
				,title: "Ending At:"
				,options: EndAtOptions
				,required: EndRequired	
				,submitOnChange: true
			)
			
			if (EndAt == "Sunrise" || EndAt == "Sunset") {
				input ("EndAtOffSet"
					,"number"
					,title: "Offset in minutes (+/-)"
					,required: false
					, submitOnChange: true
				)
			}else if (EndAt == "Specific Time") {
				input (name: "SpecificEndTime"
				, type: "time"
				, title: "Enter End Time:"
				, description: null
				, required: true
				, defaultValue: null
				, submitOnChange: true
				)
			}
		}
	}
}

def installed()
{
	initialize()
	if (state.debug){ debugLog("${app.label} child Install Complete")}
}

def updated()
{
	unsubscribe()
	initialize()
	if (allowedDays){
		state.allowedDays = allowedDays
	}else{
		state.allowedDays = null
	}
	if (allowCustomName == "False") {
		def Switches = ""
		ControlSwitches.each{individualSwitch ->
		if (Switches != "") Switches = Switches + " and "
		Switches = Switches + individualSwitch.displayName 
		}
	
		def Motions = ""
		MotionSensors.each{individualMotionSensor -> 
			if (Motions != "") Motions = Motions + " or "
			Motions = Motions + individualMotionSensor.displayName
		}
		app.updateLabel("Turn on: ${Switches} on Motion: ${Motions}")
	}else{
		app.updateLabel("${CustomName}")	
	}
	
	if (debugEnabled == "True") {
		state.debug = true
	}else{
		state.debug = null
	}
	
	if (RetriggerSafetyAppliesTo == "auto"){
		state.RetriggerSafetyAppliesTo = "auto"
	}else{
		state.RetriggerSafetyAppliesTo = "manual"
	}
	
	if (AutoOffCondition) {
		if (state.debug){ debugLog("Set AutoOffCondition Value to: ${state.AutoOffCondition}")}
		state.AutoOffCondition = AutoOffCondition.toInteger()
	}else{
		state.AutoOffCondition = null
	}
	if (AutoOffMinutes) {
		if (state.debug){ debugLog("Set AutoOffMinutes Value to: ${state.AutoOffMinutes}")}
		state.AutoOffMinutes = AutoOffMinutes.toInteger()
	}

	if (state.debug){ 
		debugLog("Set RetriggerSafetyAppliesTo to: ${state.RetriggerSafetyAppliesTo}")
		debugLog("${app.label} child Update Complete")
	}
}

def initialize()
{
	state.debug = ""
	state.vChild = "1.3.7"
	state.ReTriggerSafety = null
	parent.updateVer(state.vChild)
	subscribe(MotionSensors, "motion.inactive", MotionInactiveHandler)
	subscribe(MotionSensors, "motion.active", MotionActiveHandler)
	subscribe(ControlSwitches, "switch", SwitchHandler)
	if (state.debug){ debugLog("${app.label} child Initialize Complete")}
}

//Handles Active Motion Sensor Events
def MotionActiveHandler(evt)
{
	//unschedule kills runIn that waits for off timer to elapse to cancel off if new motion detected.
	unschedule()
	if (state.debug) {
		debugLog("Motion Active Handler Triggered")
		debugLog("Motion Sensor: ${evt.displayName} is Active")
	}
	if ((!state.RetriggerSafety || rearmCheck(state.RetriggerSafety)) && scheduleAllowed()) {
		//if motion is detected on any sensor and switch is off, turn on
		ControlSwitches.each{individualSwitch ->
			if (individualSwitch.currentState("switch").value == "off" )
			{
				if (state.debug){ debugLog("Control Switch ${individualSwitch.displayName} is: ${individualSwitch.currentState("switch").value}, switching on.")}
				individualSwitch.on()
				state.AutoOn = true
				state.ReTriggerSafety = null
			}
		}
	}
	if (state.debug){ debugLog("Motion Active Handler Ended")}
}

//Handles Inactive Motion Sensor Events
def MotionInactiveHandler(evt)
{
	if (state.debug) {
		debugLog("Motion Inactive Handler Triggered")
		debugLog("Motion Sensor: ${evt.displayName} is InActive")
	}
	
	//See if any switches are on to know if I should do anything?
	def anySwitchesOn = null
	ControlSwitches.each{individualSwitch ->
		if (individualSwitch.currentState("switch").value == "on" ){
			anySwitchesOn = true
		}
	}
	
	if (!anySwitchesOn) {
		if (state.debug){ debugLog("No Switches are on, doing nothing")}
	}
	//Test for no motion on all sensors if none then start the shutdown timer if one is set.
	if (((allMotionInactive && state.AutoOn && anySwitchesOn) || (allMotionInactive && state.AutoOffCondition && anySwitchesOn)) && state.AutoOffMinutes) { 
		debugLog("AutoOffCondition ${state.AutoOffCondition}")
		
		//Wait for Timeout to Elapse then turn all switches off if auto off criteria met
		if (state.AutoOffCondition == 1 || state.AutoOffCondition == 2 || state.AutoOffCondition == 3 || state.AutoOffCondition == 5) {
			if (state.debug){ debugLog("Auto Off Condition met, all Motion InActive going to wait for timeout of ${state.AutoOffMinutes} minutes")}
			runIn(state.AutoOffMinutes * 60 ,NoMotionTurnAllOff)
		}
		
	}
	if (state.debug){ debugLog("Motion Inactive Handler Ended")}
}

//Handles all subscribed switch events
def SwitchHandler(evt)
{
	if (state.debug){ debugLog("Switch Handler Triggered")}
	switch(evt.value)
	{
		case "on":
			if(evt.isPhysical()){
				if (state.debug) debugLog("Switch: ${evt.displayName} turned on Manually")
				state.AutoOn = false
				state.RetriggerSafety = null
				if (state.AutoOffCondition == 2 || state.AutoOffCondition > 3) {
					if (state.debug){ debugLog("Auto Off Condition met, Scheduling Off Timer going to wait for timeout of ${state.AutoOffMinutes} minutes")}
					runIn(state.AutoOffMinutes * 60 ,NoMotionTurnAllOff)
				}
				
			}else{
				if (state.debug) debugLog("Switch: ${evt.displayName} turned on Automatically")
				debugLog("Auto Off Condition: ${state.AutoOffCondition}")
				if ((state.AutoOffCondition == 2 && state.AutoOn) || state.AutoOffCondition == 3 || state.AutoOffCondition == 5) {
					if (state.debug){ debugLog("Auto Off Condition met, Scheduling Off Timer going to wait for timeout of ${state.AutoOffMinutes} minutes")}
					runIn(state.AutoOffMinutes * 60 ,NoMotionTurnAllOff)
				}
			}
			break
		case "off":
			if(evt.isPhysical()){
				if (state.debug) {debugLog("Switch: ${evt.displayName} turned off Manually")}
				state.RetriggerSafety = now()
				state.AutoOn = false
				unschedule()
			}else{
				if (state.debug) debugLog("Switch: ${evt.displayName} turned off Automatically")
				state.AutoOn = false
				if (state.RetriggerSafetyAppliesTo == "auto"){
					if (state.debug) debugLog("ReArm Trigger Enabled because applies to is set to auto")
					state.RetriggerSafety = now()
				}else{
					if (state.debug) debugLog("ReArm Trigger Not enabled because applies to is NOT set to auto")
					state.RetriggerSafety = null
				}
			}
			break
	}
	if (state.debug){ debugLog("Switch Handler Ended")}
}

//Checks if a schedule is set and if so then are we within the schedule
def scheduleAllowed(){	
	if (state.StartAt || state.EndAt || state.SpecificStartTime || state.SpecificEndTime){
		def df = new java.text.SimpleDateFormat("EEEE")
		df.setTimeZone(location.timeZone)
		def day = df.format(new Date())
		def fromTime = null
		def toTime = null
		def sunsetTime = null
		def sunriseTime = null
		
		fromTime = state.SpecificStartTime
		toTime = state.SpecificEndTime
		
		if (StartAt == "Sunrise") {
			sunriseTime = Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", location.currentValue("sunriseTime"))
			if (state.StartAtOffSet){
				fromTime = new Date(sunriseTime.time + (state.StartAtOffSet * 60 * 1000))
			}else{
				fromTime = sunriseTime
			}
		}else if (StartAt == "Sunset") {
		    sunsetTime = Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", location.currentValue("sunsetTime"))
			if (state.StartAtOffSet){
				fromTime = new Date(sunsetTime.time + (state.StartAtOffSet * 60 * 1000))
			}else{
				fromTime = sunsetTime
			}
		}
		
		if (EndAt == "Sunrise") {
			sunriseTime = Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", location.currentValue("sunriseTime"))
			if (state.EndAtOffSet){
				toTime = new Date(sunriseTime.time + (state.EndAtOffSet * 60 * 1000))
			}else{
				toTime = sunriseTime
			}
		}else if (EndAt == "Sunset") {
			sunsetTime = Date.parse("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", location.currentValue("sunsetTime"))
			if (state.EndAtOffSet){
				toTime = new Date(sunsetTime.time + (state.EndAtOffSet * 60 * 1000))
			}else{
				toTime = sunsetTime
			}
		}
		

		
		//def sunsetTime = new Date(location.currentValue("sunsetTime"))
		if (state.debug){
			debugLog("Current Day: ${day}")
			debugLog("Sunrise Time: ${sunriseTime}")
			debugLog("SunsetTime: ${sunsetTime}")
			debugLog("fromTime: ${fromTime}")
			debugLog("toTime: ${toTime}")
		}
		
		if(timeOfDayIsBetween(fromTime, toTime, (new Date()), location.timeZone)){
			if (state.debug){debugLog("Current Time is In Between Given Range")}
			if(state.allowedDays){
				if(state.allowedDays.contains(day)){
					if (state.debug){debugLog("Current Day is In Between Given Day Range")}
					return true
				}else{
					if (state.debug){debugLog("Current Day NOT In Between Given Day Range")}
					return false
				}
			}
			return true
		}else{
			if (state.debug){debugLog("Current Time NOT In Between Given Range")}
			return false
		}
	}else if(state.allowedDays){
		if(state.allowedDays.contains(day)){
			if (state.debug){debugLog("Current Day is In Between Given Day Range")}
			return true
		}else{
			if (state.debug){debugLog("Current Day NOT In Between Given Day Range")}
			return false
		}
	}
	return false
}

//Returns True if all motion sensors are inactive
def allMotionInactive(){
	def IsSensorActive = ""
	MotionSensors.each{individualMotionSensor ->
		if (individualMotionSensor.value == "active") {IsSensorActive = "True"}
	}
	if (IsSensorActive){
		return
	}else{
	return true
	}
}

//Returns True if retrigger safety timer has elapsed from last manual turn off event
def rearmCheck(LastOffTime){
	if (state.debug){ 
		debugLog("Test ReArmTrigger Elapsed Started")
		debugLog("Time Now: ${now()}")
		debugLog("Last Off Time: ${LastOffTime}")
		debugLog("Retrigger Interval: ${RetriggerSafetyInterval.toInteger()}")
	}
	if (LastOffTime) {
		if (now() - LastOffTime > RetriggerSafetyInterval.toInteger()*1000) {
			debugLog("ReArmTrigger Time Has Elapsed")
			return true
		}else{
			debugLog("ReArmTrigger Time NOT Elapsed")
			return
		}
	}else{
		debugLog("ReArmTrigger Time Null")
		return true
	}
}

//Test if No Motion is present on any of the selected motion sensors
def NoMotionTurnAllOff(){
	ControlSwitches.each{individualSwitch ->
		if (individualSwitch.currentState("switch").value == "on" ){
			if(state.AutoOffMinutes != 0) {
				if (state.debug){ debugLog("Control Switch ${individualSwitch.displayName} is: ${individualSwitch.currentState("switch").value}, switching off.")}
					individualSwitch.off()
				}
			}
		}
}

//Debug Logger
def debugLog(message){ log.debug "//Motion Based Switch Control Child Debug\\\\: $message"
}