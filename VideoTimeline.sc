VideoTimeline {
  classvar <project, <w;

  *readProject { |name|
    var project = VTProject.read(name);
    this.openProject(project);
  }

  *newProject { |name|
    var project = VTProject(name, 5000,
      markers: [],
      liveNetAddr: NetAddr("127.0.0.1", 12345),
      mediaList: VTMediaList.fromArray([
        ["blank", 5.0],
        ["A title", 45.6],
        ["Another title", 73.2],
        ["Short clip", 15.34],
        ["Accident tape", 24.3],
        ["Ferris B's Day", 20.3],
        ["Gregory falls", 25.3],
        ["Billiard balls", 24.3],
        ["Shopping malls", 23.3],
        ["HALLS cough drops advert", 15.3],
        ["Titanic", 24.3],
      ])
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

    Window.closeAll;
    w = VTWindow(project, { |name| this.readProject(name) }, {|name| this.newProject(name) });

    project.addDependant({ |what, field ...etc|
      if (doRefresh) { w.refresh; };
      if (field == \name) {
        w.projectNameText.string_(project.name);
      };
      field = etc[0];
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
    });

    OSCdef(\playhead, { |msg|
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
    OSCdef(\is_playing, { |msg|
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

    OSCdef(\time, { |msg|
      defer {
        project.markers[msg[1]] = msg[2];
        w.refresh;
      }
    }, "/time");
    OSCdef(\cue_points, { |msg|
      project.markers = nil.dup(msg[1]);
    }, "cue_points");
    OSCdef(\tempo, { |msg|
      defer { project.tempo = msg[1]; w.tempoText.string_(project.tempo.round(0.001).asString.padRight(7, "0")); }
    }, "/tempo");
  }
}