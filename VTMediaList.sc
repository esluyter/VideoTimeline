VTMediaList {
  var <mediaList;

  storeArgs { ^[mediaList]; }

  *fromArray { |array|
    ^this.new(array.collect({ |line|
      VTMediaInfo(*line);
    }));
  }

  *new { |list|
    list = list ?? [VTMediaInfo(), VTMediaInfo(1, "dummy", 60 * 5)];
    ^super.newCopyArgs(list);
  }

  at { |index|
    //^mediaList[index];
    ^mediaList.select({ |media| media.index == index })[0];
  }

  collect { |func|
    ^mediaList.collect(func);
  }

  asString {
    ^mediaList.collect({ |media| [media.index, media.name, media.duration].join(";") }).join("\n");
  }

  parseString { |string|
    mediaList = string.stripWhiteSpace.split($\n).collect({ |line| VTMediaInfo(*line.split($;)) });
    this.changed(\mediaList);
  }

  fromArray { |array|
    mediaList = array.collect { |line|
      VTMediaInfo(*line);
    };
    this.changed(\mediaList);
  }

  id { ^\mediaList }
}


VTMediaInfo {
  var <index, <name, <duration;

  storeArgs { ^[index, name, duration]; }

  *new { |index = 0, name = "blank", duration = 5.0|
    index = index.asInteger;
    duration = duration.asFloat;
    ^super.newCopyArgs(index, name, duration);
  }

  posTime { |pos|
    ^(pos * 0.01 * duration);
  }

  timePos { |time|
    ^(time / duration) * 100;
  }
}