VTClip {
  var <start, <sustain, <media, <pos, <speed, <>bus;
  var <>originStart, <>originSustain;
  classvar <>lastMedia = 1;

  storeArgs { ^[start, sustain, media, pos, speed]; }

  *new { |start, sustain, media, pos=0, speed = 1, bus|
    media = media ?? lastMedia;
    lastMedia = media;
    ^super.newCopyArgs(start, sustain, media, pos, speed, bus);
  }

  start_ { |argstart| start = argstart; this.changed(\start); }
  sustain_ { |argsustain| sustain = argsustain; this.changed(\sustain); }
  media_ { |argmedia| media = argmedia; this.class.lastMedia = media; this.changed(\media); }
  pos_ { |argpos| pos = argpos; this.changed(\pos); }
  speed_ { |argspeed| speed = argspeed; this.changed(\speed); }

  name {
    var name = bus.project.mediaList[media];
    if (name.isNil) {
      name = ""
    } {
      name = name.name
    };
    ^name;
  }

  time {
    ^bus.project.mediaList[media].posTime(pos);
  }

  timePos { |time|
    ^bus.project.mediaList[media].timePos(time);
  }

  startPos { |playhead|
    var beatsIn = (playhead - start); // how far in to clip ar we in beats?
    var timeIn = (beatsIn * (60 / bus.project.tempo));
    var adjustIn = timeIn * speed;
    ^this.timePos(this.time + adjustIn);
  }

  play { |playhead|
    var startPos = this.startPos(playhead);
    [bus.id, media, startPos, speed].postln;
    VideoTimeline.play(bus.id, media, startPos, speed);
  }

  pause { |playhead|
    [bus.id, "pause"].postln;
    VideoTimeline.pause(bus.id);
  }

  stop { |playhead|
    [bus.id, "blank"].postln;
    VideoTimeline.stop(bus.id);
  }

}

VTFuncClip {
  var <start, <sustain, <func, <>stopFunc, <>bus;
  var <>originStart, <>originSustain;

  storeArgs { ^[start, sustain, func, stopFunc]; }

  *new { |start, sustain, func, stopFunc, bus|
    func = func ?? {{ |playhead, clip, startPos| ("start the func at " ++ startPos).postln }};
    stopFunc = stopFunc ?? {{ |playhead, clip| "stop the func".postln }};
    ^super.newCopyArgs(start, sustain, func, stopFunc, bus);
  }

  start_ { |argstart| start = argstart; this.changed(\start); }
  sustain_ { |argsustain| sustain = argsustain; this.changed(\sustain); }

  media { ("oops media" ++ this.asCompileString).postln; ^0 }
  media_ { |argmedia| ("oops media_" ++ this.asCompileString).postln }
  pos { ("oops pos" ++ this.asCompileString).postln; ^0 }
  pos_ { |argpos| ("oops pos_" ++ this.asCompileString).postln }
  speed { ("oops speed" ++ this.asCompileString).postln; ^0 }
  speed_ { |argspeed| ("oops speed_" ++ this.asCompileString).postln }
  func_ { |argfunc| func = argfunc; this.changed(\func) }

  name {
    ^"func { |playhead, clip, startPos|"
  }

  funcString {
    ^func.asCompileString.findRegexp("^\\{(\\s*\\|playhead,\\sclip,\\sstartPos\\|)?[\\n\\s]*(.*)\\}$")[2][1];
  }

  stopFuncString {
    ^stopFunc.asCompileString.findRegexp("^\\{(\\s*\\|playhead,\\sclip\\|)?[\\n\\s]*(.*)\\}$")[2][1];
  }

  time {
    ("oops time" ++ this.asCompileString).postln
  }

  timePos { |time|
    ("oops timePos" ++ this.asCompileString).postln
  }

  startPos { |playhead|
    ("oops startPos" ++ this.asCompileString).postln
  }

  play { |playhead|
    func.value(playhead, this, (playhead - start * (60 / bus.project.tempo)))
  }

  pause { |playhead|
    this.stop(playhead)
  }

  stop { |playhead|
    stopFunc.value(playhead, this)
  }
}