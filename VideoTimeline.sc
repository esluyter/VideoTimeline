VideoTimeline {
  // singleton class
  classvar <project, <w, <>videoControl;

  *play { |bus_id, media, startPos, speed|
    if (videoControl.notNil) {
      videoControl.perform(bus_id).cue(media, startPos, speed);
    };
  }

  *pause { |bus_id, media, position|
    if (videoControl.notNil) {
      videoControl.perform(bus_id).pause(*if(media.notNil, { [\media, media, \position, position] }, { [] }));
    };
  }

  *stop { |bus_id|
    if (videoControl.notNil) {
      videoControl.perform(bus_id).reset;
    };
  }

  *readProject { |name|
    var project = VTProject.read(name);
    this.openProject(project);
  }

  *newProject { |name|
    var project = VTProject(name, 5000,
      markers: [],
      liveNetAddr: NetAddr("127.0.0.1", 12345),
      mediaList: VTMediaList()
    );
    this.openProject(project);
  }

  *openProject { |argproject|
    var doRefresh = true;

    if (project.notNil) {
      if (project.name != "Untitled") {
        "Saving on close".postln;
        project.write;
      } {
        "Losing any changes to Untitled".postln;
      };
      project.deepRelease;
    };
    project = argproject;

    //Window.closeAll;
    if (w.notNil) { w.win.close };
    w = VTWindow(project, { |name| this.readProject(name) }, {|name| this.newProject(name) });

    project.addDependant({ |what, field ...etc|
      //[what, field, etc].debug("Project changed: ");
      defer {
        if (doRefresh) { w.refresh; };
        if (field == \name) {
          w.projectNameText.string_(project.name);
        };
        field = etc[0];
        if (field == \mediaList) {
          w.refreshMediaList;
          w.posValue_(project.selectedClipsPos);
        };
        if (field == \selectedClips) {
          var selectedClips = project.selectedClips;
          if (selectedClips.size > 0) {
            if (selectedClips[0].class == VTClip) {
              w.inspector.visible_(true);
            } {
              w.inspectorFunc.visible_(true);
            };
            w.mediaValue_(project.selectedClipsMedia);
            w.posValue_(project.selectedClipsPos);
            w.speedValue_(project.selectedClipsSpeed);
            w.inspectorFuncText.string_(project.selectedClipsFuncString);
            w.inspectorStopFuncText.string_(project.selectedClipsStopFuncString);

            if (selectedClips.size == 1) {
              w.inspectorBgColor.background_(Color.hsv([0.28, 0.5, 0.8, 0.06][(selectedClips[0].bus.id.ascii - 97)[0]], 0.2, 0.9, 0.65));
              w.makeInspectorInvisibleButton.background_(Color.hsv([0.28, 0.5, 0.8, 0.06][(selectedClips[0].bus.id.ascii - 97)[0]], 0.2, 0.9, 0.65));
              w.makeInspectorVisibleButton.background_(Color.hsv([0.28, 0.5, 0.8, 0.06][(selectedClips[0].bus.id.ascii - 97)[0]], 0.2, 0.9, 0.65));
            } {
              w.inspectorBgColor.background_(Color.gray(0.8, 0.5));
              w.makeInspectorInvisibleButton.background_(Color.gray(0.8, 0.5));
              w.makeInspectorVisibleButton.background_(Color.gray(0.7, 0.5));
            };
          } {
            w.inspector.visible_(false);
            w.inspectorFunc.visible_(false);
            w.inspectorBgColor.background_(Color.gray(0.8, 0.5));
            w.makeInspectorInvisibleButton.background_(Color.gray(0.8, 0.5));
            w.makeInspectorVisibleButton.background_(Color.gray(0.7, 0.5));
          };
        };
        if (field == \clip) {
          var index = etc[1];
          var what = etc[2];
          if (what == \pos) {
            w.posValue_(project.selectedClipsPos);
          };
        };
      };
    });

    OSCdef(\vt_playhead, { |msg|
      project.buses.do { |bus, i|
        var nowPlayingIndex;
        project.livePlayhead = msg[1];
        bus.playClip;

        defer {
          if (project.is_playing) {
            w.newCenter(project.playhead);
          };
          w.refresh;
        }
      };
    }, "/playhead");
    OSCdef(\vt_is_playing, { |msg|
      defer {
        doRefresh = false;
        project.is_playing = msg[1].asBoolean;
        // pause currently playing clips
        if (project.is_playing.not) {
          project.buses.collect(_.wasPlayingIndex).do { |val, i|
            if (val.notNil) {
              project.buses[i].pauseClip;
            }
          };
          project.buses.do(_.wasPlayingIndex_(nil));
        };
        w.refresh;
        doRefresh = true;
      }
    }, "/is_playing");

    OSCdef(\vt_time, { |msg|
      defer {
        project.markers[msg[1]] = msg[2];
        w.refresh;
      }
    }, "/time");
    OSCdef(\vt_cue_points, { |msg|
      project.markers = nil.dup(msg[1]);
    }, "cue_points");
    OSCdef(\vt_tempo, { |msg|
      defer { project.tempo = msg[1]; w.tempoText.string_(project.tempo.round(0.001).asString.padRight(7, "0")); }
    }, "/tempo");
  }

  *clips { ^project.buses.collect(_.clips) }
}