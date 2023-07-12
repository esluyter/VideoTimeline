VTMediaList {
  var <mediaList;

  storeArgs { ^[mediaList]; }

  *fromArray { |array|
    ^this.new(array.collect({ |line, i|
      VTMediaInfo(i, *line);
    }));
  }

  *new { |list|
    list = list ?? [VTMediaInfo()]
    ^super.newCopyArgs(list);
  }

  at { |index|
    ^mediaList[index];
  }

  collect { |func|
    ^mediaList.collect(func);
  }
}


VTMediaInfo {
  var <index, <name, <duration;

  storeArgs { ^[index, name, duration]; }

  *new { |index = 0, name = "blank", duration = 5.0|
    ^super.newCopyArgs(index, name, duration);
  }

  posTime { |pos|
    ^(pos * 0.01 * duration);
  }

  timePos { |time|
    ^(time / duration) * 100;
  }
}