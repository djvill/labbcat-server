Open long sound file... /home/robert/nzilbb/labbcat-server/nzilbb/labbcat/server/task/test/text.wav
Rename... soundfile
select LongSound soundfile
Extract part... 0.075 0.225 0
Rename... sample0
select Sound sample0
formantCeiling = 5500
To Formant (burg)... 0.0025 5 formantCeiling 0.025 50
  result = Get value at time... 1 0.075 Hertz Linear
  print 'result:0'
  printline
  result = Get value at time... 2 0.075 Hertz Linear
  print 'result:0'
  printline
  Remove
select Sound sample0
Remove
select LongSound soundfile
Extract part... 2.975 4.025 0
Rename... sample1
select Sound sample1
formantCeiling = 5500
To Formant (burg)... 0.0025 5 formantCeiling 0.025 50
  result = Get value at time... 1 0.525 Hertz Linear
  print 'result:0'
  printline
  result = Get value at time... 2 0.525 Hertz Linear
  print 'result:0'
  printline
  Remove
select Sound sample1
Remove