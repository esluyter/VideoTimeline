VTWindow {
  var <win, <project;
  var <bounds, tBounds; // timeline overview bounds
  var <t; // timeline overview
  var <v, <w, <x, <y; // clip view
  var <tempoText, playheadResetButton, playheadToSelectionStartButton, playheadToSelectionEndButton, markerButton, newClipButtons, splitButton, moveClipsButton, moveTimeButton, undoButton, redoButton, saveButton, saveAsButton, newButton, openButton, <projectNameText, trimPlayheadButton, trimClipsButton;
  var <>displayedTime = 300;
  var <>startTime = -20;
  var dragFrom;
  var oldVal = nil;
  var openFunc, newFunc;
  var <inspector, <inspectorBg, <inspectorBgColor, <makeInspectorVisibleButton, <makeInspectorInvisibleButton;
  var <mediaMenu, <posBox, <timeText, <speedBox;
  var <inspectorFunc, <inspectorFuncText, <inspectorStopFuncText;
  var waitForInteractionFinishRout;

  refresh {
    y.refresh;
    x.refresh;
    w.refresh;
    v.refresh;
    t.refresh;
  }

  save { if (project.name == "Untitled") { this.saveAs } { project.write }; }

  saveAs { this.textPrompt("Save Project As", "Please type a name to save the project as:", { |name| project.name_(name); project.write }) }

  open { this.optionsPrompt("Open Project", "Please choose a project to open:", VTProject.projectNames, project.name, openFunc) }

  new { this.textPrompt("New Project", "Please type a name for the new project:", newFunc) }


  absX { |time| ^(time / project.projectTime) * tBounds.width }

  newCenter { |time|
    ^startTime = time - (displayedTime * 0.5);
  }
  oldCenter { ^startTime + (displayedTime / 2) }

  *new { |project, openFunc, newFunc, bounds|
    ^super.new.init(bounds, project, openFunc, newFunc);
  }

  newButton { |parent, bounds, text, action|
    ^Button(parent, bounds)
    .states_([[text, Color.gray(0.5), Color.gray(0.85)]])
    .font_(Font("Helvetica", 13, true))
    .action_(action);
  }

  newTopButton { |text, left, width = 120, action|
    ^Button(win, Rect(left, 5, width, 21)).states_([[text, Color.gray(0.5), Color.gray(0.85)]]).font_(Font("Helvetica", 13, true)).action_(action);
  }

  textPrompt { |title = "Name", prompt = "Please enter some text:", action|
    var width = 500;
    var height = 200;
    var screenCenter = Window.screenBounds.width / 2;
    var okFunc = {
      if (textField.string != "") {
        action.(textField.string); nameWin.close;
      } {
        "Just say something!".postln;
      };
    };
    var cancelFunc = { nameWin.close; };
    var nameWin = Window(title, Rect(screenCenter - (width / 2), 50, width, height))
    .alwaysOnTop_(true)
    .background_(Color.hsv(0.65, 0.1, 0.65))
    .front;
    var promptText = StaticText(nameWin, Rect(50, 20, width - 100, 50))
    .font_(Font("Helvetica", 18, true))
    .stringColor_(Color.gray(0.3))
    .string_(prompt)
    .align_(\center);
    var textField = TextField(nameWin, Rect(100, 75, width - 200, 40))
    .font_(Font("Helvetica", 18))
    .align_(\center)
    .stringColor_(Color.gray(0.1))
    .background_(Color.gray(0.9))
    .keyDownAction_({ |view, char, modifiers, unicode, keycode, key|
      if (key == 16777220) { //enter
        okFunc.()
      };
      if (key == 16777216) { // esc
        cancelFunc.()
      };
    });
    var okButt = this.newButton(nameWin, Rect(100, 145, (width / 2) - 105, 21), "OK", okFunc);
    var cancelButt = this.newButton(nameWin, Rect((width / 2) + 5, 145, (width / 2) - 105, 21), "Cancel", cancelFunc);
  }

  optionsPrompt { |title = "Name", prompt = "Please select from these options:", optionsList, selectedOption, action|
    var width = 500;
    var height = 200;
    var screenCenter = Window.screenBounds.width / 2;
    var okFunc = {
      action.(options.item); nameWin.close;
    };
    var cancelFunc = { nameWin.close; };
    var nameWin = Window(title, Rect(screenCenter - (width / 2), 50, width, height))
    .alwaysOnTop_(true)
    .background_(Color.hsv(0.65, 0.1, 0.65))
    .front;
    var promptText = StaticText(nameWin, Rect(50, 20, width - 100, 50))
    .font_(Font("Helvetica", 18, true))
    .stringColor_(Color.gray(0.3))
    .string_(prompt)
    .align_(\center);
    var options = PopUpMenu(nameWin, Rect(100, 75, width - 200, 40))
    .items_(optionsList)
    .value_(optionsList.collect(_.asSymbol).indexOf(selectedOption.asSymbol) ?? 0)
    .font_(Font("Helvetica", 18))
    .stringColor_(Color.gray(0.1))
    .background_(Color.gray(0.9));
    var okButt = this.newButton(nameWin, Rect(100, 145, (width / 2) - 105, 21), "OK", okFunc)
    .keyDownAction_({ |view, char, modifiers, unicode, keycode, key|
      if (key == 16777220) { //enter
        okFunc.()
      };
      if (key == 16777216) { // esc
        cancelFunc.()
      };
    });
    var cancelButt = this.newButton(nameWin, Rect((width / 2) + 5, 145, (width / 2) - 105, 21), "Cancel", cancelFunc);
  }

  init { |argbounds, argproject, argopenFunc, argnewFunc|
    project = argproject;
    bounds = argbounds ?? Rect(0, 0, Window.screenBounds.width, 250);
    tBounds = bounds.copy.origin_(10@30).height_(20).width_(bounds.width - 20);
    startTime = project.projectTime * -0.02;
    displayedTime = (project.projectTime * 1.1) - startTime;
    win = Window("Timeline", bounds, false, false).acceptsMouseOver_(true).front;

    openFunc = argopenFunc;
    newFunc = argnewFunc;

    // top panel
    View(win, Rect(0, 0, bounds.width, 30)).background_(Color.hsv(0.65, 0.1, 0.58));

    tempoText = StaticText(win, Rect(10, 7, 80, 17))
    .font_(Font("Helvetica", 15))
    .string_(project.tempo)
    .stringColor_(Color.gray(0.4))
    .background_(Color.gray(0.7))
    .align_(\center);

    playheadResetButton = this.newTopButton("| Live", 100, 40, { project.resetPlayhead; });
    playheadToSelectionStartButton = this.newTopButton("| ←", 145, 30, { project.playheadToSelectionStart });
    playheadToSelectionEndButton = this.newTopButton("→ |", 180, 30, { project.playheadToSelectionEnd });
    markerButton = this.newTopButton("Markers", 220, 70, { project.liveNetAddr.sendMsg("/markers", 1) });

    View(win, Rect(370, 0, 270, 30)).background_(Color.hsv(0.65, 0.1, 0.65));
    StaticText(win, Rect(385, 6, 100, 21)).string_("New Clip In").font_(Font("Helvetica", 13, true)).stringColor_(Color.gray(0.4));
    newClipButtons = [
      this.newTopButton("A", 465, 30, { project.a.addClip(project.playhead); }),
      this.newTopButton("B", 500, 30, { project.b.addClip(project.playhead); }),
      this.newTopButton("C", 535, 30, { project.c.addClip(project.playhead); }),
      this.newTopButton("D", 570, 30, { project.d.addClip(project.playhead); })
    ];

    splitButton = this.newTopButton("Split", 655, 50, { project.splitSelectedClips(project.playhead) });
    moveClipsButton = this.newTopButton("Move Clips", 715, 90, {
      project.moveSelectedClipsTo(project.playhead);
    });
    moveTimeButton = this.newTopButton("Move Time", 810, 90, {
      var selectionStart = project.selectedClipRange()[0];
      if (project.playhead > selectionStart) {
        project.addTime(selectionStart, project.playhead);
      } {
        project.cutTime(project.playhead, selectionStart);
      };
    });

    undoButton = this.newTopButton("Undo", 975, 60, { project.undo });
    redoButton = this.newTopButton("Redo", 1040, 60, { project.redo });
    projectNameText = StaticText(win, Rect(1110, 7, 250, 17)).font_(Font("Helvetica", 15)).string_(project.name).stringColor_(Color.gray(0.4)).background_(Color.gray(0.7)).align_(\center).mouseDownAction_({ |view, x, y, modifiers, buttonNumber, clickCount| if (clickCount == 2) { this.open } });
    saveButton = this.newTopButton("Save", 1370, 60, { this.save });
    saveAsButton = this.newTopButton("Save As", 1435, 80, { this.saveAs });
    openButton = this.newTopButton("Open", 1525, 60, { this.open });
    newButton = this.newTopButton("New", 1590, 50, { this.new });

    trimPlayheadButton = this.newTopButton("Trim to Playhead", bounds.width - 240, 120, { project.trimToPlayhead; });
    trimClipsButton = this.newTopButton("Trim to Clips", bounds.width - 110, 100, { project.trimToClips; });

    // timeline overview
    View(win, Rect(0, 30, 10, 20)).background_(Color.hsv(0.6, 0.1, 0.68));
    t = UserView(win, tBounds.copy.width_(tBounds.width + 10));
    t.drawFunc = { |view|
      Pen.fillColor = Color.gray(0.7);
      Pen.strokeColor = Color.gray(0.5);
      Pen.width = 2;
      Pen.addRect(Rect(this.absX(startTime), 2, this.absX(displayedTime), 15));
      Pen.fillStroke;
      project.buses.do { |bus, i|
        bus.clips.do { |clip|
          Color.hsv([0.28, 0.5, 0.8, 0.06].at(i), 0.1, 0.6).set;
          Pen.addRect(Rect(this.absX(clip.start), 4 + (i * 3) + if (i == 0) { 0 } { 2 }, this.absX(clip.sustain), if (i == 0) { 4 } { 3 }));
          Pen.fill;
        }
      };
      Color.gray(0.3).set;
      Pen.line(this.absX(project.playhead)@0, this.absX(project.playhead)@18);
      Pen.stroke;
      Color.gray(0.75).set;
      Pen.line(0@19, (tBounds.width + 10)@19);
      Pen.stroke;
    };
    t.mouseDownAction = { |view, x, y, modifiers, buttonNumber, clickCount|
      if (clickCount == 2) {
        this.newCenter(x / tBounds.width * project.projectTime);
      };
      dragFrom = x@y;
      oldVal = this.oldCenter()@displayedTime;
      this.refresh;
    };
    t.mouseMoveAction = { |view, x, y, modifiers, buttonNumber, clickCount|
      var yFactor = (1.05.pow(dragFrom.y - y));
      var xDelta = x - dragFrom.x;
      displayedTime = min(project.projectTime * 1.5, oldVal.y * yFactor);
      this.newCenter((oldVal.x + (xDelta / tBounds.width * project.projectTime)).clip(0, project.projectTime));
      this.refresh;
    };

    y = VTBusView(this, bounds.copy.origin_(0@194).height_(60), project.d);
    x = VTBusView(this, bounds.copy.origin_(0@146).height_(60), project.c);
    w = VTBusView(this, bounds.copy.origin_(0@98).height_(60), project.b);
    v = VTBusView(this, bounds.copy.origin_(0@50).height_(60), project.a);


    // --------- beginning of inspector -----------

    inspectorBg = View(win, Rect(bounds.width - 185, 60, 185, bounds.height - 50))
    .visible_(false);

    inspectorBgColor = View(inspectorBg, Rect(0, 25, 185, bounds.height - 100))
    .background_(Color.gray(0.8, 0.5));

    inspector = View(inspectorBg, Rect(3, 0, 180, bounds.height - 50))
    .visible_(false);

    StaticText(inspector, Rect(120, 30, 150, 25))
    .string_("media")
    .font_(Font("Helvetica", 15))
    .stringColor_(Color.gray(0.45))
    .align_(\left);

    mediaMenu = PopUpMenu(inspector, Rect(5, 50, 170, 25))
    .items_(project.mediaList.collect({ |media| media.index.asString ++ ". " ++ media.name })/*["blank", "1 - Whatever", "2 - Something else"]*/)
    .value_(0)
    .font_(Font("Helvetica", 13, true))
    .stringColor_(Color.gray(0.4))
    .background_(Color.gray(0.75))
    .action_({
      project.selectedClipsMedia_(mediaMenu.value);
      this.mediaValue_(mediaMenu.value);
      project.addUndoStep;
    });

    timeText = StaticText(inspector, Rect(10, 80, 100, 25))
    .string_((0 * 60).asTimeString)
    .font_(Font("Helvetica", 15))
    .stringColor_(Color.gray(0.45))
    .align_(\left);

    StaticText(inspector, Rect(120, 100, 45, 25))
    .string_("pos")
    .font_(Font("Helvetica", 15))
    .stringColor_(Color.gray(0.45))
    .align_(\left);

    posBox = NumberBox(inspector, Rect(10, 100, 100, 25))
    .value_(0.0)
    .step_(0.1)
    .scroll_step_(0.01)
    .shift_scale_(5)
    .font_(Font("Helvetica", 15, true))
    .stringColor_(Color.gray(0.4))
    .normalColor_(Color.gray(0.4))
    .background_(Color.gray(0.85, 0.5))
    .action_({
      project.selectedClipsPos_(posBox.value);
      this.posValue_(posBox.value);
      waitForInteractionFinishRout.stop;
      waitForInteractionFinishRout = fork {
        0.1.wait;
        project.addUndoStep;
      };
    });

    StaticText(inspector, Rect(120, 135, 45, 25))
    .string_("speed")
    .font_(Font("Helvetica", 15))
    .stringColor_(Color.gray(0.45))
    .align_(\left);

    speedBox = NumberBox(inspector, Rect(10, 135, 100, 25))
    .value_(1.0)
    .step_(0.1)
    .scroll_step_(0.01)
    .shift_scale_(5)
    .font_(Font("Helvetica", 15, true))
    .stringColor_(Color.gray(0.4))
    .normalColor_(Color.gray(0.4))
    .background_(Color.gray(0.85, 0.5))
    .action_({
      project.selectedClipsSpeed_(speedBox.value);
      this.speedValue_(speedBox.value);
      waitForInteractionFinishRout.stop;
      waitForInteractionFinishRout = fork {
        0.1.wait;
        project.addUndoStep;
      };
    });

    inspectorFunc = View(inspectorBg, Rect(3, 0, 180, bounds.height - 50))
    .visible_(false);

    inspectorFuncText = TextView(inspectorFunc, Rect(0, 30, 180, bounds.height - 140));
    inspectorStopFuncText = TextView(inspectorFunc, Rect(0, bounds.height - 108, 128, 40));
    this.newButton(inspectorFunc, Rect(130, bounds.height - 105, 50, 25), "Save", {
      project.selectedClipsFunc_(("{ |playhead, clip, startPos| " ++ inspectorFuncText.string ++ " }").interpret)
      .selectedClipsStopFunc_(("{ |playhead, clip| " ++ inspectorStopFuncText.string ++ " }").interpret);
      project.addUndoStep;
    });

    makeInspectorInvisibleButton = StaticText(inspectorBg, Rect(155, 5, 30, 20))
    .string_("→")
    .align_(\center)
    .font_(Font("Helvetica", 20, true))
    .stringColor_(Color.gray(0.45))
    .background_(Color.gray(0.8, 0.5))
    .mouseDownAction_({ inspectorBg.visible_(false); makeInspectorVisibleButton.visible_(true); });

    makeInspectorVisibleButton = StaticText(win, Rect(bounds.width - 28, 65, 30, 20))
    .string_("←")
    .align_(\center)
    .font_(Font("Helvetica", 20, true))
    .stringColor_(Color.gray(0.45))
    .background_(Color.gray(0.8, 0.5))
    .mouseDownAction_({ inspectorBg.visible_(true); makeInspectorVisibleButton.visible_(false); })
    .visible_(true);

    // -------- end of inspector --------

  }

  openInspector { if (inspectorBg.visible.not) { inspectorBg.visible_(true); makeInspectorVisibleButton.visible_(false); } }

  mediaValue_{ |value|
    mediaMenu.value_(value);
    if (posBox.value.isNaN.not and: (mediaMenu.value.notNil)) {
      timeText.string_((project.mediaList[mediaMenu.value].posTime(posBox.value) * 60).asTimeString);
    } {
      timeText.string_("");
    };
  }

  posValue_ { |value|
    posBox.value_(value);
    if (value.isNaN) {
      timeText.stringColor_(timeText.stringColor.copy.alpha_(0));
      posBox.normalColor_(posBox.normalColor.copy.alpha_(0));
    } {
      timeText.stringColor_(timeText.stringColor.copy.alpha_(1));
      if (mediaMenu.value.notNil) {
        timeText.string_((project.mediaList[mediaMenu.value].posTime(value) * 60).asTimeString);
      } {
        timeText.string_("");
      };
      posBox.normalColor_(posBox.normalColor.copy.alpha_(1));
    };
  }

  speedValue_ { |value|
    speedBox.value_(value);
    if (value.isNaN) {
      speedBox.normalColor_(speedBox.normalColor.copy.alpha_(0));
    } {
      speedBox.normalColor_(speedBox.normalColor.copy.alpha_(1));
    };
  }
}