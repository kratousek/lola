package com.tomst.lolly.core;

// signalizace z threadu do hlavni formy
public enum TDevState {
    tInit, tStart, tWaitForAdapter, tFirmware, tHead, tSerial, tGetTime,tCapacity,
    tCompareTime,  tSetTime, tCheckTMSFirmware, tInfo, tMeasure,tWaitForMeasure, tReadMeteo,tSetMeteo,
    tSerialDuplicity,
    tProgress,tReadData,tLollyService,tFinishedData, tRemainDays,tWaitInLimbo,tNoHardware,tFinal,tError
}