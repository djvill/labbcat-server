Open long sound file... /home/robert/nzilbb/labbcat-server/nzilbb/labbcat/server/task/test/text.wav
Rename... soundfile
select LongSound soundfile
Extract part... 0.1 0.2 0
Rename... sample0
select Sound sample0
intensityPitchFloor = 60
To Intensity... intensityPitchFloor 0 yes
result = Get maximum... 0 0.1 Parabolic
print 'result'
printline
Remove
select Sound sample0
Remove
select LongSound soundfile
Extract part... 3 4 0
Rename... sample1
select Sound sample1
intensityPitchFloor = 60
To Intensity... intensityPitchFloor 0 yes
result = Get maximum... 0 1 Parabolic
print 'result'
printline
Remove
select Sound sample1
Remove