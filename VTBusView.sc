VTBusView {
  var <vtWindow, <bus;

  var hoverClip; // [index, {\left, \right}]
  var addingClip;
  var clickPlayhead, clickY, clickIndex;
  var <view;

  winPosX { |time| ^((time - vtWindow.startTime) / vtWindow.displayedTime)  * vtWindow.bounds.width }
  winWidthX { |time| ^(time / vtWindow.displayedTime) * vtWindow.bounds.width }
  winXTime { |x| ^vtWindow.startTime + ((x / vtWindow.bounds.width) * vtWindow.displayedTime) }
  newXTime { |x, time| ^vtWindow.startTime = time - ((x / vtWindow.bounds.width) * vtWindow.displayedTime) }

  clipHoverAtPoint { |x|
    var ret;
    bus.clips.do { |clip, i|
      var clipStart = this.winPosX(clip.start);
      var clipEnd = clipStart + this.winWidthX(clip.sustain);
      if (x.inRange(clipStart, clipStart + 5)) {
        ret = [i, \left];
      };
      if (x.inRange(clipEnd - 5, clipEnd)) {
        ret = [i, \right];
      };
    };
    ^ret;
  }

  *new { |vtWindow, bounds, bus|
    ^super.new.init(vtWindow, bounds, bus);
  }

  refresh {
    view.refresh;
  }

  init { |argvtWindow, bounds, argbus|
    var v;
    var clipHeight = bounds.height - 20;
    vtWindow = argvtWindow;
    bus = argbus;
    // close up bus / clip view
    v = UserView(vtWindow.win, bounds).background_(Color.gray(0.92));

    v.drawFunc = { |view|
      var hue = [0.28, 0.5, 0.8, 0.06].at((bus.id.ascii - 97)[0]);
      Pen.use {
        // clips
        bus.clips.do { |clip, i|
          var selected = bus.selectedClips.indexOf(i).notNil;
          if (selected) {
            Color.hsv(hue, 0.2, 0.95).set;
          } {
            Color.hsv(hue, 0.3, 0.5).set;
          };
          Pen.addRect(Rect(this.winPosX(clip.start), 15, this.winWidthX(clip.sustain), clipHeight));
          Pen.fill;
          Pen.strokeColor = Color.gray(0.9);
          Pen.width = 1;
          Pen.addRect(Rect(this.winPosX(clip.start) + 1, 15, this.winWidthX(clip.sustain) - 1, clipHeight));
          Pen.stroke;
          Pen.strokeColor = Color.hsv(hue, 0.3, 0.5);
          Pen.addRect(Rect(this.winPosX(clip.start), 14, this.winWidthX(clip.sustain) + 1, clipHeight + 2));
          Pen.stroke;
          if (selected) {
            //Pen.strokeColor = Color.hsv(0.28, 1, 0.7);
            Pen.strokeColor = Color.hsv(hue, 0.3, 0.5, 0.5);
            Pen.width = 2;
            Pen.addRect(Rect(this.winPosX(clip.start), 15, this.winWidthX(clip.sustain), clipHeight));
            Pen.stroke;
            Pen.fillColor = Color.white;
            Pen.addRect(Rect(this.winPosX(clip.start) + 4, 19, this.winWidthX(clip.sustain) - 8, 16));
            Pen.fill;
          };
          Pen.use {
            Pen.addRect(Rect(this.winPosX(clip.start), 15, this.winWidthX(clip.sustain) - 4, clipHeight));
            Pen.clip;
            if (selected) {
              Color.black.set;
            } {
              Color.hsv(hue, 0.1, 0.9).set;
            };
            if (bus.clipIndexAtPoint(bus.project.playhead) == i and: bus.project.is_playing) {
              Color.red.set;
            };
            Pen.font = Font("Helvetica", 11, true);
            if (clip.class == VTClip) {
              Pen.stringAtPoint("% % %".format(clip.media, clip.pos.round(0.001), clip.speed), (this.winPosX(clip.start) + 7)@23);
            } {
              Pen.stringAtPoint(clip.funcString, (this.winPosX(clip.start) + 7)@23);
            };
            Pen.font = Font("Helvetica", 20);
            Color.gray(if(selected) { 0.0 } { 1.0 }, 0.15).set;
            Pen.stringAtPoint(clip.name, this.winPosX(clip.start)@35)
          };
        };
        if (hoverClip.notNil) {
          var clip = bus.clips[hoverClip[0]];
          if (bus.selectedClips.indexOf(hoverClip[0]).notNil) {
            Pen.fillColor = Color.hsv(hue, 0.3, 0.5, 0.5);
          } {
            Pen.fillColor = Color.hsv(hue, 0.2, 0.95, 0.5);
          };
          if (hoverClip[1] == \left) {
            Pen.addRect(Rect(this.winPosX(clip.start), 35, 5, clipHeight - 20));
          };
          if (hoverClip[1] == \right) {
            Pen.addRect(Rect(this.winPosX(clip.start) + this.winWidthX(clip.sustain) - 5, 35, 5, clipHeight - 20));
          };
          Pen.fill;
        };
        // playhead
        //Color.hsv(0.6, 0.1, 0.68).set;
        Color.gray(0.3).set;
        Pen.addRect(Rect(this.winPosX(bus.project.playhead), 0, 2, bounds.height));
        Pen.fill;
        // out of bounds
        Color.hsv(0.6, 0.2, 0.75).set;
        Pen.addRect(Rect(this.winPosX(0) - bounds.width, 0, bounds.width, bounds.height));
        Pen.fill;
        Color.hsv(hue, 0.3, 0.75, 0.15).set;
        Pen.addRect(Rect(this.winPosX(0) - bounds.width, 0, bounds.width, bounds.height));
        Pen.fill;
        Color.hsv(hue, 0.3, 0.5, 0.5).set;
        Pen.stringAtPoint(bus.id.asString, (this.winPosX(0) - 30)@(bounds.height / 1.2 - 34), Font("Helvetica", 50));
        { // timeline bars
          var tickIncrement = (vtWindow.displayedTime / 200).ceil.asInteger;
          var startRound = max(vtWindow.startTime.round(tickIncrement), 0);
          var endRound = (vtWindow.startTime + vtWindow.displayedTime).round(tickIncrement);
          Pen.width = 1;
          (startRound, startRound + tickIncrement..endRound).do { |i|
            if (i % (tickIncrement * 4) == 0) {
              Pen.moveTo(this.winPosX(i)@0);
              Pen.lineTo(this.winPosX(i)@10);
              Pen.strokeColor = Color.gray(0.6);
              Pen.stroke;
            } {
              if (vtWindow.displayedTime < 150) {
                Pen.moveTo(this.winPosX(i)@0);
                Pen.lineTo(this.winPosX(i)@7);
                Pen.strokeColor = Color.gray(0.65);
                Pen.stroke;
              };
            };
            if (i % (tickIncrement * if (vtWindow.displayedTime < 100) { 4 } { 16 }) == 0) {
              Pen.stringAtPoint("%".format((i / 4 + 1).asInteger), (this.winPosX(i) + 2)@(-2));
            };
          };
        }.();
        // markers
        bus.project.markers.do { |time|
          if (time.notNil) {
            var x = this.winPosX(time);
            Color.gray(1.0, 0.5).set;
            Pen.line(x@0, x@100);
            Pen.width = 2;
            Pen.stroke;
            Pen.moveTo((x)@100);
            Pen.lineTo((x + 13)@95);
            Pen.lineTo((x)@88);
            Pen.strokeColor = Color.gray(1.0, 0.5);
            Pen.fillColor = Color.hsv(0.6, 0.6, 0.8, 1);
            Pen.fillStroke;
            Color.hsv(0.6, 1, 1, 1).set;
            Pen.line(x@0, x@100);
            Pen.width = 1.25;
            Pen.stroke;
          };
        };
      }
    };

    v.mouseDownAction = { |view, x, y, modifiers, buttonNumber, clickCount|
      var clipIndex = bus.clipIndexAtPoint(this.winXTime(x));
      clickPlayhead = this.winXTime(x);
      clickY = y;
      clickIndex = clipIndex;
      if (clickCount == 1) { addingClip = nil };
      // if we're below the time marks
      if (clickY > 15) {
        if (modifiers.isShift) {
          var indexOfClipInSelection = bus.selectedClips.indexOf(clipIndex).postln;
          if (indexOfClipInSelection.isNil) {
            bus.selectedClips = (bus.selectedClips ++ clipIndex.asArray).asSet.asArray;
          } {
            bus.selectedClips.removeAt(indexOfClipInSelection);
            bus.changed(\selectedClips);
          };
        } {
          if (clickY > 35 or: (bus.selectedClips.indexOf(clipIndex).isNil)) {
            bus.project.deselectAll;
            bus.selectedClips = clipIndex.asArray;
          };
        };
        bus.project.selectedClips.do { |clip|
          clip.originStart = clip.start;
          clip.originSustain = clip.sustain;
        };
        // double click to add a clip or open inspector
        if (clickCount == 2) {
          if (bus.selectedClips == []) {
            addingClip = bus.addClip(clickPlayhead);
          } {
            vtWindow.openInspector;
          };
        }
      } {
        // adjust live playhead
        bus.project.playhead = clickPlayhead;
        //if (modifiers.isShift.not) {
          bus.project.liveNetAddr.sendMsg("/start_time", clickPlayhead);
          if (bus.project.is_playing) { bus.project.buses.do(_.updateClipForPlayhead) };
        //};

        // shift to scrub
        if (modifiers.isShift) {
          bus.project.buses.do(_.scrub);
        };
      };
    };
    v.mouseUpAction = { |view, x, y, modifiers, buttonNumber|
      var undoable = false;
      bus.project.selectedClips.do { |clip|
        if (clip.originStart != clip.start or: (clip.originSustain != clip.sustain)) { undoable = true };
      };
      if (addingClip.notNil) { undoable = true };
      if (undoable) {
        bus.project.addUndoStep;
      };
    };
    v.mouseMoveAction = { |view, x, y, modifiers, buttonNumber, clickCount|
      var delta = this.winXTime(x) - clickPlayhead; // how much mouse has moved
      if (addingClip.notNil) {
        // adjust sustain of added clip
        var clip = bus.clips[addingClip];
        var proposedSustain = max(0, this.winXTime(x) - clip.start);
        if (bus.clipIndicesInRange(clip.start, clip.start + proposedSustain) == [addingClip]) {
          clip.sustain = proposedSustain;
          // adjust project time if we're past end
          if ((clip.start + clip.sustain) > bus.project.projectTime) {
            bus.project.projectTime = clip.start + clip.sustain;
          };
        };
      } {
        if (hoverClip.notNil) {
          // adjust clip start/end
          var clip = bus.clips[hoverClip[0]];
          if (hoverClip[1] == \left) {
            var clipEnd = clip.originStart + clip.originSustain;
            var proposedStart = (clip.originStart + delta).clip(0, clipEnd);
            if (bus.clipIndicesInRange(proposedStart, clipEnd) == [hoverClip[0]]) {
              // adjust clip video pos
              clip.pos = clip.startPos(proposedStart);
              clip.start = proposedStart;
              clip.sustain = clipEnd - clip.start;
            };
          };
          if (hoverClip[1] == \right) {
            var proposedSustain = max(0, clip.originSustain + delta);
            var clipIndices = bus.clipIndicesInRange(clip.start, clip.start + proposedSustain);
            if (clipIndices == [hoverClip[0]]) { // no conflicts
              clip.sustain = proposedSustain;
            } {
              clipIndices.reject({ |item| item == hoverClip[0] }).do { |clipIndex|
                var iClip = bus.clips[clipIndex];
                if (iClip.start <= (clip.start + proposedSustain)) {
                  proposedSustain = iClip.start - clip.start;
                };
              };
              clip.sustain = proposedSustain;
            };
            // adjust project time if we're past end
            if ((clip.start + clip.sustain) > bus.project.projectTime) {
              bus.project.projectTime = clip.start + clip.sustain;
            };
          };
        } {
          if (clickY > 15) {
            if (clickY > 35 or: (clickIndex.isNil)) {
              // "selection drag" select all clips in range
              if (modifiers.isShift) {
                bus.selectedClips = (bus.selectedClips ++ bus.clipIndicesInRange(clickPlayhead, this.winXTime(x))).asSet.asArray;
              } {
                if (modifiers.isAlt) {
                  bus.project.buses.do { |bus|
                    bus.selectedClips = bus.clipIndicesInRange(clickPlayhead, this.winXTime(x));
                  };
                } {
                  bus.selectedClips = bus.clipIndicesInRange(clickPlayhead, this.winXTime(x));
                };
              };
            } {
              // "movement drag" all selected clips
              bus.project.moveSelectedClipsDelta(delta);
            };
          } {
            bus.project.playhead = this.winXTime(x);
            //if (modifiers.isShift.not) {
              bus.project.liveNetAddr.sendMsg("/start_time", this.winXTime(x));
            //};
            // shift to scrub
            if (modifiers.isShift) {
              bus.project.buses.do(_.scrub);
            };
          };
        };
      };
    };
    v.mouseWheelAction = { |view, x, y, modifiers, xDelta, yDelta|
      if (yDelta != 0.0) {
        var yFactor = 1.05.pow(yDelta * -1);
        var xTime = this.winXTime(x);
        vtWindow.displayedTime = min(bus.project.projectTime * 1.5, vtWindow.displayedTime * yFactor);
        this.newXTime(x, xTime);
      };
      {
        var oldCenter = (vtWindow.startTime + (vtWindow.displayedTime / 2));
        vtWindow.newCenter((oldCenter - (xDelta * (vtWindow.displayedTime / 500))).clip(0, bus.project.projectTime));
      }.value;
      vtWindow.refresh;
    };
    v.mouseOverAction = { |view, x, y|
      var oldHoverClip = hoverClip.copy;
      if (y > 35) {
        hoverClip = this.clipHoverAtPoint(x);
      } {
        hoverClip = nil;
      };
      if (oldHoverClip != hoverClip) {
        v.refresh;
      };
    };
    v.keyDownAction = { |view, char, modifiers, unicode, keycode, key|
      if (key == 32) {
        bus.project.liveNetAddr.sendMsg("/spacebar", 1);
      };
      if (key == 16777219) { // delete
        if (bus.project.selectedClips != []) {
          bus.project.deleteSelectedClips;
          bus.project.addUndoStep;
        };
      };
      if (modifiers.isAlt) { // alt-arrow / alt-cmd-arrow
        if (key == 16777234) {
          if (modifiers.isCmd) {
            bus.project.playheadToSelectionStart;
          } {
            bus.project.liveNetAddr.sendMsg("/jump", 0);
          };
        };
        if (key == 16777236) {
          if (modifiers.isCmd) {
            bus.project.playheadToSelectionEnd;
          } {
            bus.project.liveNetAddr.sendMsg("/jump", 1);
          };
        };
      };
      if (modifiers.isCmd) {
        if (key == 90) { // Z
          if (modifiers.isShift) {
            bus.project.redo;
          } {
            bus.project.undo;
          };
        };
        if (key == 83) { // S
          if (modifiers.isShift) {
            vtWindow.saveAs
          } {
            vtWindow.save
          };
        };
        if (key == 79) { // O
          vtWindow.open
        };
        if (key == 78) { // N
          vtWindow.new
        };
        if (key == 69) { // E
          bus.project.splitSelectedClips(bus.project.playhead)
        };
        if (key == 65) { // A
          bus.project.selectAll;
        };
        if (key == 76 and: modifiers.isAlt) { // cmd-alt-L
          vtWindow.makeInspectorVisibleButton.visible_(vtWindow.inspectorBg.visible);
          vtWindow.inspectorBg.visible_(vtWindow.inspectorBg.visible.not);
        };
        if (49 <= key and: (key <= 52)) { // 1 - 4
          var index = key - 49;
          bus.project.buses[index].addClip(bus.project.playhead);
        };
      }
    };


    view = v;
  }
}