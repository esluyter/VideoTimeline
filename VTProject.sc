VTProject {
  var <name, <projectTime, <>markers, <tempo, <playhead, <liveNetAddr;
  var <undoSteps, <redoSteps;
  var <mediaList;

  var <livePlayhead;
  var <is_playing = false;
  var <>maxUndo = 50;
  var <a, <b, <c, <d;
  var <unsaved = false;

  storeArgs { ^[name, projectTime, this.buses, markers, tempo, playhead, liveNetAddr, undoSteps, redoSteps, mediaList]; }

  write {
    if (name == "Untitled") {
      "Please save as a different name.".postln;
    } {
      this.asCompileString.write(("projects" +/+ name ++ ".txt").resolveRelative, true).write(("projects/backups" +/+ name ++ "_" ++ Date.getDate.stamp ++ ".txt").resolveRelative);
      unsaved = false;
      this.changed(\unsaved);
    };
  }

  *read { |name| ^("projects" +/+ name ++ ".txt").resolveRelative.load }

  *projectNames { ^PathName("projects".resolveRelative).files.collect(_.fileNameWithoutExtension) }

  *new { |name, projectTime, buses, markers, tempo = 120, playhead = 0, liveNetAddr, undoSteps, redoSteps, mediaList|
    undoSteps = undoSteps ?? [];
    redoSteps = redoSteps ?? [];
    markers = markers ?? [];
    mediaList = mediaList ?? VTMediaList();
    buses = buses ? [];
    ^super.newCopyArgs(name, projectTime, markers, tempo, playhead, liveNetAddr, undoSteps, redoSteps, mediaList).init(buses);
  }

  init { |buses|
    # a, b, c, d = buses;
    a = a ?? VTBus(\a);
    b = b ?? VTBus(\b);
    c = c ?? VTBus(\c);
    d = d ?? VTBus(\d);
    this.buses.do(_.project_(this));
    this.buses.do(_.addDependant(this));
    mediaList.addDependant(this);
    this.addUndoStep;
    unsaved = false;
  }

  update { |object ... args|
    this.changed(object.id, *args);
  }

  // information

  buses { ^[a, b, c, d] }

  clipIndicesInRange { |startTime, endTime|
    ^this.buses.collect(_.clipIndicesInRange(startTime, endTime)).flat;
  }

  clipsInRange { |startTime, endTime|
    ^this.buses.collect { |bus| bus.clips[bus.clipIndicesInRange(startTime, endTime)] }.flat;
  }

  selectedClips {
    ^this.buses.collect{ |bus| bus.clips[bus.selectedClips] }.flat;
  }

  selectedClipRange {
    var rangeStart = projectTime, rangeEnd = 0;
    this.selectedClips.do { |clip|
      rangeStart = min(rangeStart, clip.start);
      rangeEnd = max(rangeEnd, clip.start + clip.sustain);
    };
    ^[rangeStart, rangeEnd];
  }

  selectedClipsMedia {
    var media = this.selectedClips.select({ |clip| clip.class == VTClip }).collect(_.media).asSet.asArray;
    if (media.size == 1) {
      ^media[0]
    } {
      ^(-1)
    };
  }

  selectedClipsPos {
    var pos = this.selectedClips.select({ |clip| clip.class == VTClip }).collect(_.pos).asSet.asArray;
    if (pos.size == 1) {
      ^pos[0]
    } {
      ^(0/0)
    }
  }

  selectedClipsSpeed {
    var speed = this.selectedClips.select({ |clip| clip.class == VTClip }).collect(_.speed).asSet.asArray;
    if (speed.size == 1) {
      ^speed[0]
    } {
      ^(0/0)
    }
  }

  selectedClipsFuncString {
    var func = this.selectedClips.select({ |clip| clip.class == VTFuncClip }).collect(_.funcString).asSet.asArray;
    if (func.size == 1) {
      ^func[0]
    } {
      ^""
    }
  }

  selectedClipsStopFuncString {
    var func = this.selectedClips.select({ |clip| clip.class == VTFuncClip }).collect(_.stopFuncString).asSet.asArray;
    if (func.size == 1) {
      ^func[0]
    } {
      ^""
    }
  }

  // actions

  deepRelease {
    this.buses.do(_.release);
    this.buses.do(_.deepRelease);
  }

  addUndoStep {
    undoSteps = undoSteps.add(this.buses.collect(_.clips).asCompileString);
    if (undoSteps.size > maxUndo) {
      undoSteps = undoSteps[undoSteps.size - maxUndo..];
    };
    redoSteps = [];
    unsaved = true;
    this.changed(\unsaved);
  }

  undo {
    var clips;
    if (undoSteps.size > 1) {
      redoSteps = redoSteps.add(undoSteps.pop);
      clips = undoSteps.last.interpret;
      this.buses.do { |bus, i|
        bus.clips_(clips[i]);
      };
    };
    unsaved = true;
    this.changed(\unsaved);
  }

  redo {
    var clips;
    if (redoSteps.size > 0) {
      undoSteps = undoSteps.add(redoSteps.pop);
      clips = undoSteps.last.interpret;
      this.buses.do { |bus, i|
        bus.clips_(clips[i]);
      };
    };
    unsaved = true;
    this.changed(\unsaved);
  }

  deselectAll {
    this.buses.do(_.selectedClips_([]));
  }

  moveSelectedClipsDelta { |delta|
    this.buses.do(_.moveSelectedClipsDelta(delta));
  }

  moveSelectedClipsTo { |newStart|
    var oldRange, delta, newEnd, noConflicts = true;
    oldRange = this.selectedClipRange();
    delta = newStart - oldRange[0];
    newEnd = newStart + oldRange[1] - oldRange[0];
    // if there are no conflicting clips, relocate the entire selection
    this.buses.do { |bus|
      var range = bus.selectedClipRange;
      if ((range[1] - range[0]).isPositive) {
        bus.clips[bus.clipIndicesInRange(*range)].do { |clip|
          if (this.selectedClips.indexOf(clip).isNil) { noConflicts = false };
        }
      }
    };
    if (noConflicts) {
      this.selectedClips.do { |clip|
        clip.start = clip.start + delta;
      };
      // adjust project time if we're past end
      if (newEnd > projectTime) {
        projectTime = newEnd;
      };
    };
    this.changed(\clips);
  }

  deleteSelectedClips {
    this.buses.do(_.deleteSelectedClips);
  }

  name_ { |argname|
    name = argname;
    this.changed(\name);
  }

  projectTime_ { |argtime|
    projectTime = argtime;
    this.changed(\projectTime);
  }

  tempo_ { |argtempo|
    tempo = argtempo;
    this.changed(\tempo);
  }

  playhead_ { |argplayhead|
    playhead = argplayhead;
    this.changed(\playhead);
  }

  livePlayhead_ { |argplayhead|
    playhead = livePlayhead = argplayhead;
  }

  resetPlayhead { this.playhead_(livePlayhead) }

  is_playing_ { |argis_playing|
    is_playing = argis_playing;
    this.changed(\is_playing);
  }

  addTime { |startTime, endTime|
    var delta = endTime - startTime;
    var affectedClips = this.buses.collect(_.clipsAfter(startTime)).flat;
    affectedClips.do { |clip| clip.start = clip.start + delta };
    this.projectTime_(projectTime + delta);
  }

  cutTime { |startTime, endTime|
    var delta = endTime - startTime;
    var affectedClips = this.buses.collect(_.clipsAfter(endTime)).flat;
    if (this.clipIndicesInRange(startTime, endTime) == []) {
      affectedClips.do { |clip| clip.start = clip.start - delta };
      this.projectTime_(projectTime - delta);
    } {
      "Please clear clips first!".postln;
    };
  }

  selectAll {
    this.buses.do { |bus|
      bus.selectedClips = (0 .. bus.clips.size - 1);
    }
  }

  splitSelectedClips { |time|
    this.selectedClips.do { |clip|
      this.buses.do(_.splitClips(time));
    }
  }

  trimToPlayhead { this.projectTime_(playhead); }
  trimToClips {
    projectTime = 4;
    [a, b, c].collect(_.clips).flat.do { |clip| projectTime = max(projectTime, clip.start + clip.sustain); };
    this.changed(\projectTime);
  }

  playheadToSelectionStart {
    var range = this.selectedClipRange;
    if (range[1] > range[0]) {
      this.playhead_(range[0]);
      liveNetAddr.sendMsg("/start_time", this.playhead);
    };
  }

  playheadToSelectionEnd {
    var range = this.selectedClipRange;
    if (range[1] > range[0]) {
      this.playhead_(range[1]);
      liveNetAddr.sendMsg("/start_time", this.playhead);
    };
  }

  selectedClipsMedia_ { |media| this.selectedClips.select({ |clip| clip.class == VTClip }).do(_.media_(media)); }
  selectedClipsPos_ { |pos| this.selectedClips.select({ |clip| clip.class == VTClip }).do(_.pos_(pos)) }
  selectedClipsSpeed_ { |speed| this.selectedClips.select({ |clip| clip.class == VTClip }).do(_.speed_(speed)) }
  selectedClipsFunc_ { |func| this.selectedClips.select({ |clip| clip.class == VTFuncClip }).do(_.func_(func)) }
  selectedClipsStopFunc_ { |func| this.selectedClips.select({ |clip| clip.class == VTFuncClip }).do(_.stopFunc_(func)) }

  unsaved_ { |val| unsaved = val; this.changed(\unsaved) }
}