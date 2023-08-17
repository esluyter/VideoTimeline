VTBus {
  var <id, <clips, <>project;
  var <selectedClips;
  var <>wasPlayingIndex;
  var <pausedClip;

  storeArgs { ^[id, clips] ;}

  *new { |id = \a, clips, project|
    clips = clips ?? [];
    ^super.newCopyArgs(id, clips, project).init;
  }

  init {
    clips.do(_.bus_(this));
    clips.do(_.addDependant(this));
    selectedClips = [];
  }

  update { |object, param|
    this.changed(\clip, clips.indexOf(object), param);
  }

  // information

  clipIndexAtPoint { |time|
    clips.do { |clip, i|
      if ((clip.start <= time) and: (clip.start + clip.sustain >= time)) {
        ^i;
      };
    };
    ^nil;
  }

  clipIndicesInRange { |start, end|
    var ret = [];
    clips.do { |clip, i|
      var endIsInRange = (clip.start + clip.sustain > start) and: (clip.start + clip.sustain <= end);
      var startIsInRange = (clip.start >= start) and: (clip.start < end); // changed from <= end so cut time will work....
      var clipIsHuge = (clip.start < start) and: (clip.start + clip.sustain > end);
      if (endIsInRange or: startIsInRange or: clipIsHuge) {
        ret = ret.add(i);
      };
    };
    ^ret;
  }

  clipsAfter { |startTime|
    ^clips.select { |clip| clip.start >= startTime }
  }

  clipRange { |clipIndexArray, field = \start|
    var rangeStart = project.projectTime, rangeEnd = 0;
    clips[clipIndexArray].do { |clip|
      if (field == \originStart and: clip.originStart.isNil) {
        clip.originStart = clip.start;
      };
      rangeStart = min(rangeStart, clip.perform(field));
      rangeEnd = max(rangeEnd, clip.perform(field) + clip.sustain);
    };
    ^[rangeStart, rangeEnd];
  }

  selectedClipRange { |field = \start|
    ^this.clipRange(selectedClips, field);
  }

  // action

  deepRelease {
    clips.do(_.release);
  }

  selectedClips_ { |argclips|
    if (argclips != selectedClips) {
      selectedClips = argclips;
      this.changed(\selectedClips);
    }
  }

  moveSelectedClipsTo { |newStart|
    var oldRange, delta, newEnd, noConflicts = true;
    oldRange = this.selectedClipRange();
    delta = newStart - oldRange[0];
    newEnd = newStart + oldRange[1] - oldRange[0];
    // if there are no conflicting clips, relocate the entire selection
    this.clipIndicesInRange(newStart, newEnd).do { |index|
      if (selectedClips.indexOf(index).isNil) { noConflicts = false };
    };
    if (noConflicts) {
      selectedClips.do { |index|
        var clip = clips[index];
        clip.start = clip.start + delta;
      };
      // adjust project time if we're past end
      if (newEnd > project.projectTime) {
        project.projectTime = newEnd;
      };
    };
    this.changed(\clips);
  }

  moveSelectedClipsDelta { |delta|
    // "movement drag" drag all selected clips
    var newStart = max(0, (this.selectedClipRange(\originStart) + delta)[0]);
    this.moveSelectedClipsTo(newStart);
  }

  clips_ { |argclips|
    clips.do(_.release);
    clips = argclips;
    this.init;
    this.changed(\clips);
  }

  addClip { |startTime, sustain = 30|
    var class = if (id == \d) { VTFuncClip } { VTClip };
    // returns index of added clip, or nil
    var interference = this.clipIndicesInRange(startTime, startTime + sustain);
    if (interference == []) {
      var clip = class.new(startTime, sustain, bus: this).addDependant(this);
      clips = clips.add(clip);
      this.changed(\clips)
      ^clips.size - 1;
    } {
      var interferenceStart = this.clipRange(interference)[0];
      if (interferenceStart > startTime) {
        var clip = class.new(startTime, interferenceStart - startTime, bus: this).addDependant(this);
        clips = clips.add(clip);
        this.changed(\clips);
        ^clips.size - 1;
      } {
        "There's already a clip there!".postln;
        ^nil;
      };
    };
  }

  splitClips { |time, allowUnselected = false|
    var index = this.clipIndexAtPoint(time);
    if (this.selectedClips.indexOf(index).notNil or: allowUnselected) {
      var clip = clips[index];
      if (clip.start < time) {
        var copy = clip.copy;
        clip.sustain = time - clip.start;
        copy.sustain = copy.sustain - clip.sustain;
        copy.start = time;
        copy.pos = clip.startPos(time);
        copy.addDependant(this);
        clips = clips.add(copy);
        this.changed(\clips);
      };
    };
  }

  deleteSelectedClips {
    selectedClips.sort.reverse.do { |i|
      clips[i].release;
      clips.removeAt(i);
    };
    selectedClips = [];
    //this.changed(\clips);
    this.changed(\selectedClips);
  }

  playClip { |force = false|
    var nowPlayingIndex = this.clipIndexAtPoint(project.playhead);
    if (project.is_playing) {
      if (nowPlayingIndex != wasPlayingIndex) {
        if (nowPlayingIndex.notNil) {
          var clip = clips[nowPlayingIndex];
          clip.play(project.playhead);
        } {
          this.stopClip;
        };
      };
      // blank bus on play after pause
      if (pausedClip.class == VTClip and: nowPlayingIndex.isNil) {
        pausedClip.stop(project.playhead);
        pausedClip = nil;
      };
      if (force) {
        if (nowPlayingIndex.notNil) {
          var clip = clips[nowPlayingIndex];
          clip.play(project.playhead);
        };
      };
      wasPlayingIndex = nowPlayingIndex;
    };
  }

  updateClipForPlayhead {
    var nowPlayingIndex = this.clipIndexAtPoint(project.playhead);
    if (nowPlayingIndex.notNil) {
      var clip = clips[nowPlayingIndex];
      if (nowPlayingIndex == wasPlayingIndex) {
        if (clip.class == VTFuncClip) {
          clip.stop(project.playhead);
        };
        this.playClip(true);
      };
    };
  }

  pauseClip {
    if (wasPlayingIndex.notNil) {
      clips[wasPlayingIndex].pause(project.playhead);
      pausedClip = clips[wasPlayingIndex];
    };
  }

  stopClip {
    if (wasPlayingIndex.notNil) {
      clips[wasPlayingIndex].stop(project.playhead);
      pausedClip = nil;
    };
  }

  scrub {
    if (project.isPlaying.not and: [\a, \b, \c].indexOf(id).notNil) {
      var nowPlayingIndex = this.clipIndexAtPoint(project.playhead);
      if (nowPlayingIndex.notNil) {
        var clip = clips[nowPlayingIndex];
        var startPos = clip.startPos(project.playhead);
        VideoTimeline.pause(id, clip.media, startPos);
      } {
        VideoTimeline.stop(id);
      }
    };
  }
}