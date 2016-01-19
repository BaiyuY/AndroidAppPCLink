**********************************************************************************************
*                                                                                            *
*           Copyright (C) 2014 TaiDoc Technology Corporation. All rights reserved.           *
*           PCLinkLibrary for Android v1.2.7                                                 * 
*                                                                                            *
**********************************************************************************************

PCLinkLibrary API is a convenient tool to communicate the meter in android application. 
It support android app right now. In the image, includes 3 folders:
1. lib: there is a jar file in the folder, you need to import this jar to your android project.
2. Javadoc: this folder include the PCLinkLibrary javadoc, you can find the detail description about PCLinklibrary.
3. doc: there is a guide.pdf file which descripts how to use PCLinkLibrary.
4. sample demo: include the demo apk file and the sample source code.

Support Meter
/* 2-in-1 */
TD3252, TD3258, TD3261, TD3250, TD3280, TD3129

/* BG */
TD4256, TD4257, TD4272, TD4283, TD4282

/* BP */
TD3132, TD3128, TD3140

/* IRT */
TD1261, TD1035

/* SpO2 */
TD8002, TD8201, TD8255,

/* Weight Scale */
TD2500, TD2501 , TD2551 , TD2552, TD2555

Change log

2016/01/04 v1.2.7
Read system date issue fixes.

2015/11/06 v1.2.6
1.Remove Bluetooth device name filtering for supported meters
 

2015/06/25 v1.2.5
Fix minor bugs 

2015/06/18 v1.2.4
Add supporting models

2015/05/19 v1.2.3
Read measurement data issue fixes

2015/05/12 v1.2.2
1. Support command 47
3. Fix minor bugs

2015/02/17 v1.2.1
1. Support command 46
2. Support 1035
3. Fix minor bugs

2014/12/16 v1.2.0
1. Support BLE.
2. Support multiple user.
3. Support 2555
4. Support KNV V125

2014/10/09 v1.1.9
1.Add 8255 as supported models.
2. Add supported bt chip for MediaTek

2014/07/24 v1.1.8
1.Add 4282, 3129 and 3140 as supported models.

2014/01/28 v1.1.7
1.增加TD-4257支援

2014/01/17 v1.1.6
1.把3280的建構子改到2合1的地方,原本它放在bp的建構子會造成資料判斷錯誤
2.3261和3280建構子用3261的,因為它的user要取1, 原本是取0, 這樣會造成切換user時資料重新輸入,應只能輸入第一個user
3.spO2的部份要資料原本是49 command, 改為26 command

2013/11/11 v1.1.5
- Added support for IHB command.

2013/11/05 v1.1.4
- Resolve model 3280 unidentified problem.
- Modify year data judgment (filtered before 3 bit).

2013/07/24 v1.1.3
- Resolve model 8201 unidentified problem.

2013/07/15 v1.1.2
- Added support for model 3128

2012/11/23 v1.1.1
- Added the demo sample source code and apk.

2012/09/07 v1.1.0
- Support meter TD3250.

2012/09/04 v1.0.9
- Added the sendBACommand() in TD4283, it's the unique command for TD4283.

2012/09/03 v1.0.8
- Improve the bluetooth compatibility.

2012/08/07 v1.0.7
- Added the log to illustrate the meter connect by listen (type ii) or connect( type i) mode.

2012/07/06 v1.0.6
- Support the "FORA" series bluetooth meter devices.

2012/06/28 v1.0.5
- Prevent crash when android device bluetooth was turn off in listen mode.

2012/06/21 v1.0.4
- Improve this android bluetooth connection quality and stability.
- Improve meter communication quality and stability.

2012/05/29 v1.0.3
- add close bluetooth module function in BluetoothUtil.

2012/05/24 v1.0.2
- Support weight scale TD2551(W310), TD2500(W100), TD2501(W300), TD2552(W320)

2012/04/23 v1.0.1
- Support BA command for TD4283 to test bluetooth distence.

2011/12/28 v1.0.0
- First realse v1.0.0
