close all;
clear all;
clc;
javaaddpath('.');
sourcePath = 'C:\MyTemp\oma\Timon\tyo\LURU2012\Seuranta_2011_8_CMJ\';
dataFile = 'EK020147_CMJ.cfs';
cfsData = cfsReader.CFSReader([sourcePath dataFile]);
for i  = 1:length(cfsData.datasection)
    data = cfsData.datasection(i).channelData;
    plot(data{1}+data{4});
    samplingRate = 1/cfsData.datasection(i).dschannelInformation(1).xIncrement
    pause
end