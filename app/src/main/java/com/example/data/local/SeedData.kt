package com.example.data.local

import com.example.data.model.PinoutItem
import com.example.data.model.RagDocument
import com.example.data.model.RepurposeMatrixItem
import com.example.data.model.SalvageItem
import com.example.data.model.SalvageProject

object SeedData {

    val INITIAL_INVENTORY = listOf(
        SalvageItem(
            name = "TP4056 1A Li-Ion Charger IC",
            category = "Power/BMS",
            quantity = 6,
            minQuantityAlert = 2,
            sourceDevice = "Broken Vapes & Power Banks",
            condition = "Tested Working",
            estimatedValueUsd = 1.20,
            specs = "Vin: 4.5V-5.5V, Vbat: 4.2V, Iprog: 1A (1.2kΩ), SOP-8",
            pinCount = 8,
            notes = "Standard CC/CV lithium charger. Swap R3 resistor to set charging current."
        ),
        SalvageItem(
            name = "ESP32-WROOM-32D (Wi-Fi/BLE SoC)",
            category = "Microcontroller",
            quantity = 3,
            minQuantityAlert = 1,
            sourceDevice = "Discarded Smart Home Plug",
            condition = "Pristine / Desoldered",
            estimatedValueUsd = 4.50,
            specs = "Dual-core Xtensa 240MHz, 4MB Flash, 520KB SRAM, 38-Pin",
            pinCount = 38,
            notes = "Hot air desoldered cleanly. Flash wiped with esptool and tested."
        ),
        SalvageItem(
            name = "18650 Li-Ion Cell 2600mAh",
            category = "Power/BMS",
            quantity = 8,
            minQuantityAlert = 3,
            sourceDevice = "Lenovo ThinkPad Battery Pack",
            condition = "Tested Working",
            estimatedValueUsd = 3.00,
            specs = "3.7V Nominal, 4.2V Peak, Internal Resistance: 32mΩ, 2550mAh measured",
            pinCount = 2,
            notes = "All 8 cells cycled on LiitoKala tester. Capacity > 92% of nominal."
        ),
        SalvageItem(
            name = "SSD1306 0.96\" I2C OLED Display",
            category = "Displays",
            quantity = 2,
            minQuantityAlert = 1,
            sourceDevice = "Broken E-Bike Telemetry Dash",
            condition = "Tested Working",
            estimatedValueUsd = 3.50,
            specs = "128x64 pixels, I2C 0x3C, 3.3V-5V compatible, 4-Pin Header",
            pinCount = 4,
            notes = "Bright blue pixels, no burn-in. Works on Wire (SDA/SCL)."
        ),
        SalvageItem(
            name = "CH340G USB to UART Bridge",
            category = "ICs",
            quantity = 4,
            minQuantityAlert = 2,
            sourceDevice = "Damaged 3D Printer Mainboard",
            condition = "Requires Testing",
            estimatedValueUsd = 1.00,
            specs = "SOP-16, Requires 12MHz crystal between XI/XO, 5V/3.3V selectable",
            pinCount = 16,
            notes = "Ideal for custom BadUSB and Arduino flashing dongles."
        ),
        SalvageItem(
            name = "NE555 Precision Timer IC",
            category = "ICs",
            quantity = 7,
            minQuantityAlert = 2,
            sourceDevice = "Old CRT TV Deflection Board",
            condition = "Tested Working",
            estimatedValueUsd = 0.50,
            specs = "DIP-8 & SOP-8, 4.5V-16V, Astable / Monostable clock generator",
            pinCount = 8,
            notes = "Classic indestructible timer for PWM, audio alarms and logic probes."
        ),
        SalvageItem(
            name = "MP1584 High-Efficiency Buck Converter",
            category = "Power/BMS",
            quantity = 3,
            minQuantityAlert = 1,
            sourceDevice = "CCTV Security Camera Power Module",
            condition = "Tested Working",
            estimatedValueUsd = 1.80,
            specs = "Vin: 4.5V-28V, Vout: 0.8V-20V, 3A max, 1.5MHz switching",
            pinCount = 4,
            notes = "Trimmer potentiometer sets exact voltage for 5V and 3.3V rails."
        ),
        SalvageItem(
            name = "USB-C 16-Pin Receptacle",
            category = "Connectors",
            quantity = 12,
            minQuantityAlert = 4,
            sourceDevice = "Faulty USB-C Hubs & Earbud Cases",
            condition = "Tested Working",
            estimatedValueUsd = 0.75,
            specs = "Supports 5V 3A, CC1/CC2 5.1kΩ pull-downs, D+/D- USB 2.0",
            pinCount = 16,
            notes = "Must solder 5.1kΩ pull-down resistors to CC pins for modern PD chargers."
        ),
        SalvageItem(
            name = "nRF24L01+ 2.4GHz RF Transceiver",
            category = "RF/Wireless",
            quantity = 2,
            minQuantityAlert = 1,
            sourceDevice = "Scrapped RC Drone Transmitter",
            condition = "Tested Working",
            estimatedValueUsd = 2.20,
            specs = "2.4GHz ISM, 250kbps-2Mbps, SPI Interface, 1.9V-3.6V (5V tolerant logic)",
            pinCount = 8,
            notes = "Include 100uF capacitor across VCC/GND to prevent RF brownouts."
        ),
        SalvageItem(
            name = "NEMA 17 Bipolar Stepper Motor",
            category = "Motors/Drivers",
            quantity = 2,
            minQuantityAlert = 1,
            sourceDevice = "Disassembled Inkjet Office All-in-One",
            condition = "Tested Working",
            estimatedValueUsd = 7.00,
            specs = "1.8 deg/step, 12V-24V, 4-Wire Bipolar (A+, A-, B+, B-)",
            pinCount = 4,
            notes = "Torque tested. Pair with A4988 or TMC2208 salvaged stepper drivers."
        )
    )

    val INITIAL_PINOUTS = listOf(
        PinoutItem(
            id = "esp32-wroom",
            name = "ESP32-WROOM-32D",
            chipCode = "ESP32-D0WDQ6",
            packageType = "Module (38-Pin)",
            voltageRange = "3.0V - 3.6V (3.3V Nominal)",
            description = "Flagship dual-core IoT Wi-Fi 802.11 b/g/n + Bluetooth 4.2 BR/EDR & BLE SoC from Espressif.",
            pinDiagram = """
                 ┌─────────── ESP32-WROOM-32D ───────────┐
            3V3  │ [ 1]                             [38] │ GND
             EN  │ [ 2]                             [37] │ IO23 (MOSI)
           IO36  │ [ 3] VP                          [36] │ IO22 (SCL)
           IO39  │ [ 4] VN                          [35] │ TXD0 (TX)
           IO34  │ [ 5]                             [34] │ RXD0 (RX)
           IO35  │ [ 6]                             [33] │ IO21 (SDA)
           IO32  │ [ 7]                             [32] │ NC
           IO33  │ [ 8]                             [31] │ IO19 (MISO)
           IO25  │ [ 9]                             [30] │ IO18 (SCK)
           IO26  │ [10]                             [29] │ IO5  (CS)
           IO27  │ [11]                             [28] │ IO17 (TX2)
           IO14  │ [12]                             [27] │ IO16 (RX2)
           IO12  │ [13] (BOOT FAIL IF HIGH)         [26] │ IO4
            GND  │ [14]                             [25] │ IO0  (BOOT FLASH PIN)
           IO13  │ [15]                             [24] │ IO2  (STRAP LED)
            SD2  │ [16]                             [23] │ IO15
            SD3  │ [17]                             [22] │ SD1
           CMD   │ [18]                             [21] │ SD0
                 └───────────────────────────────────────┘
            """.trimIndent(),
            pinDetailsJson = "1: 3V3 Power, 2: EN Enable (Pull-up with 10k), 14: GND, 25: IO0 (Pull-down to GND for UART Flash boot mode), 33: IO21 (I2C SDA), 36: IO22 (I2C SCL), 34: RXD0, 35: TXD0",
            hackerTip = "BOOT TRAP: If IO12 is pulled high at boot, flash voltage shifts to 1.8V and chip crashes. Ground IO0 during reset to enter ROM bootloader. IO34-IO39 are INPUT ONLY (no internal pull-ups).",
            antiHallucinationStatus = "VERIFIED",
            googleDork = "\"ESP32-WROOM-32D\" filetype:pdf (datasheet | schematic | \"pin layout\")",
            datasheetUrl = "https://docs.espressif.com/projects/esp-idf/en/latest/esp32/hw-reference/esp32/get-started-devkitc.html",
            suggestedDatasheetUrl = "https://www.espressif.com/sites/default/files/documentation/esp32-wroom-32d_esp32-wroom-32u_datasheet_en.pdf"
        ),
        PinoutItem(
            id = "tp4056",
            name = "TP4056 Linear Li-Ion Charger",
            chipCode = "TP4056 / 4056E",
            packageType = "SOP-8 (Exposed Pad)",
            voltageRange = "4.0V - 8.0V (Vin), 4.2V ±1% (Vbat)",
            description = "Constant-Current/Constant-Voltage linear single-cell 3.7V lithium-ion/polymer charger with thermal feedback.",
            pinDiagram = """
                 ┌────────── TP4056 (SOP-8) ──────────┐
           TEMP  │ [1]                            [8] │ CE (Chip Enable)
           PROG  │ [2]                            [7] │ CHRG (Open-Drain LED)
            GND  │ [3]                            [6] │ STDBY (Done LED)
            VCC  │ [4]                            [5] │ BAT (Battery Pos +)
                 └────────────────────────────────────┘
                       [PAD: Thermal Ground GND]
            """.trimIndent(),
            pinDetailsJson = "1: TEMP (Thermistor / GND if unused), 2: PROG (Current set resistor to GND: 1.2k=1A, 2k=580mA, 10k=130mA), 3: GND, 4: VCC, 5: BAT (+3.7V Cell), 6: STDBY (Green/Blue LED), 7: CHRG (Red LED), 8: CE (High=Enable)",
            hackerTip = "NEVER power TP4056 without BAT connected or open-circuit voltage spikes may damage sensitive protection ICs. Tie TEMP to GND if not using an NTC thermistor. Change Rprog (pin 2) to 5kΩ when salvaging tiny 200mAh vape cells!",
            antiHallucinationStatus = "VERIFIED",
            googleDork = "\"TP4056\" (\"NanJing Top Power\" | \"datasheet\") (schematic | \"Rprog\") filetype:pdf",
            datasheetUrl = "https://dlnmh9ip6v2uc.cloudfront.net/datasheets/Prototyping/TP4056.pdf",
            suggestedDatasheetUrl = "https://dlnmh9ip6v2uc.cloudfront.net/datasheets/Prototyping/TP4056.pdf"
        ),
        PinoutItem(
            id = "ne555",
            name = "NE555 / LM555 Precision Timer",
            chipCode = "NE555P / SE555",
            packageType = "DIP-8 / SOP-8",
            voltageRange = "4.5V - 16.0V (200mA Output Source/Sink)",
            description = "The immortal analog timing circuit capable of producing accurate time delays or oscillations from microseconds to hours.",
            pinDiagram = """
                 ┌─────────── NE555 (DIP-8) ──────────┐
            GND  │ [1]                            [8] │ VCC (4.5V - 16V)
        TRIGGER  │ [2]                            [7] │ DISCHARGE
         OUTPUT  │ [3]                            [6] │ THRESHOLD
          RESET  │ [4]                            [5] │ CONTROL VOLTAGE
                 └────────────────────────────────────┘
            """.trimIndent(),
            pinDetailsJson = "1: GND, 2: TRIGGER (Starts timing cycle when < 1/3 VCC), 3: OUTPUT (High/Low drive up to 200mA), 4: RESET (Active Low, tie to VCC if unused), 5: CONTROL (0.01uF cap to GND for noise immunity), 6: THRESHOLD (Resets flip-flop when > 2/3 VCC), 7: DISCHARGE (Open collector to GND), 8: VCC",
            hackerTip = "For a 50% duty cycle square wave generator, feed OUTPUT (pin 3) through a single resistor back into pins 2 and 6 with a single capacitor to GND. Bypasses the traditional Ra/Rb dual diode requirement!",
            antiHallucinationStatus = "VERIFIED",
            googleDork = "\"NE555\" (\"Texas Instruments\" | \"Signetics\") datasheet filetype:pdf",
            datasheetUrl = "https://www.ti.com/lit/ds/symlink/ne555.pdf",
            suggestedDatasheetUrl = "https://www.ti.com/lit/ds/symlink/ne555.pdf"
        ),
        PinoutItem(
            id = "usbc-16pin",
            name = "USB Type-C 16-Pin Receptacle",
            chipCode = "TYPE-C-16P (Salvage)",
            packageType = "Surface Mount + Through-Hole Shell",
            voltageRange = "5.0V (Standard) / Up to 20V (with PD controller)",
            description = "Modern universal reversible connector for power and USB 2.0 data. Found in smartphones, earbuds, hubs.",
            pinDiagram = """
                 ┌──────────────── USB-C 16-PIN ────────────────┐
           [A1/B12] GND   [A4/B9] VBUS   [A5] CC1   [A6] D+
           [A7] D-        [A8] SBU1      [B5] CC2   [B8] SBU2
           [B7] D-        [B6] D+        [B4/A9] VBUS [B1/A12] GND
                 └──────────────────────────────────────────────┘
            """.trimIndent(),
            pinDetailsJson = "GND (Pins A1, A12, B1, B12), VBUS (Pins A4, A9, B4, B9), CC1 (Pin A5), CC2 (Pin B5), D+ (Pins A6, B6 shorted), D- (Pins A7, B7 shorted), SBU1/SBU2 (Sideband)",
            hackerTip = "THE 5.1kΩ RULE: If you wire VBUS and GND without soldering a 5.1kΩ pull-down resistor from CC1 to GND and CC2 to GND, smart USB-C Power Delivery chargers will output 0V because they think nothing is plugged in!",
            antiHallucinationStatus = "VERIFIED",
            googleDork = "\"USB Type-C\" \"16-pin\" receptacle (\"CC1\" | \"5.1k\") pinout filetype:pdf",
            datasheetUrl = "https://www.usb.org/document-library/usb-type-cr-cable-and-connector-specification-revision-23",
            suggestedDatasheetUrl = "https://www.usb.org/document-library/usb-type-cr-cable-and-connector-specification-revision-23"
        ),
        PinoutItem(
            id = "ch340g",
            name = "CH340G USB to Serial Interface",
            chipCode = "CH340G / WCH",
            packageType = "SOP-16",
            voltageRange = "3.3V - 5.0V",
            description = "Ubiquitous JiangSu QinHeng USB serial bridge found on Arduino clones, NodeMCU, and 3D printer boards.",
            pinDiagram = """
                 ┌────────── CH340G (SOP-16) ─────────┐
            GND  │ [ 1]                          [16] │ VCC
            TXD  │ [ 2]                          [15] │ R* / DTR (Autoreset)
            RXD  │ [ 3]                          [14] │ RTS
             V3  │ [ 4] (0.1uF cap to GND)       [13] │ CTS
             UD+ │ [ 5] (USB D+)                 [12] │ DSR
             UD- │ [ 6] (USB D-)                 [11] │ RI
            GND  │ [ 7]                          [10] │ DCD
             XI  │ [ 8] (12MHz Crystal)          [ 9] │ XO (12MHz Crystal)
                 └────────────────────────────────────┘
            """.trimIndent(),
            pinDetailsJson = "1: GND, 2: TXD (Out to MCU RX), 3: RXD (In from MCU TX), 4: V3 (Decouple 100nF to GND for 5V operation, tie to VCC for 3.3V), 5: UD+, 6: UD-, 8 & 9: XI & XO (12MHz Crystal with 22pF caps), 15: DTR (Connected to Arduino RESET via 100nF)",
            hackerTip = "CH340G REQUIRES external 12MHz crystal on pins 8 and 9. If you salvage CH340N or CH340C instead, they have internal oscillators and pin 8/9 are unused/different!",
            antiHallucinationStatus = "VERIFIED",
            googleDork = "\"CH340G\" (\"WCH\" | \"QinHeng\") datasheet pinout filetype:pdf",
            datasheetUrl = "https://www.mpja.com/download/35227cpdata.pdf",
            suggestedDatasheetUrl = "https://www.mpja.com/download/35227cpdata.pdf"
        ),
        PinoutItem(
            id = "ssd1306-oled",
            name = "SSD1306 0.96\" I2C OLED",
            chipCode = "SSD1306 / 128x64",
            packageType = "4-Pin Header Breakout",
            voltageRange = "3.3V - 5.5V (Integrated Charge Pump)",
            description = "Monochrome graphic OLED display with built-in controller and charge pump. Standard 0x3C I2C slave address.",
            pinDiagram = """
                 ┌──────── SSD1306 I2C ────────┐
                 │  [GND] [VCC] [SCL] [SDA]    │
                 │ ┌─────────────────────────┐ │
                 │ │ 128 x 64 Dot Matrix     │ │
                 │ │ OLED Display Screen     │ │
                 │ └─────────────────────────┘ │
                 └─────────────────────────────┘
            """.trimIndent(),
            pinDetailsJson = "1: GND (Ground), 2: VCC (3.3V to 5V), 3: SCL (I2C Clock - ESP32 IO22), 4: SDA (I2C Data - ESP32 IO21)",
            hackerTip = "Address jumper on rear: Short 0x3C for default address (0111100b), move 0Ω SMD resistor to 0x3D to run TWO displays on the same I2C bus with zero collision!",
            antiHallucinationStatus = "VERIFIED",
            googleDork = "\"SSD1306\" datasheet (\"I2C address\" | \"0x3C\" | \"charge pump\") filetype:pdf",
            datasheetUrl = "https://cdn-shop.adafruit.com/datasheets/SSD1306.pdf",
            suggestedDatasheetUrl = "https://cdn-shop.adafruit.com/datasheets/SSD1306.pdf"
        ),
        PinoutItem(
            id = "lm358",
            name = "LM358 Dual Operational Amplifier",
            chipCode = "LM358 / LM358P / LM358N",
            packageType = "DIP-8 / SOIC-8",
            voltageRange = "3.0V - 32.0V (Single Supply) / ±1.5V - ±16V (Dual)",
            description = "Industry-standard dual low-power operational amplifier designed specifically to operate from a single supply over a wide range of voltages.",
            pinDiagram = """
                 ┌────────── LM358 (DIP-8) ──────────┐
           OUT1  │ [1]                           [8] │ VCC+ (Supply 3V-32V)
           IN1-  │ [2] (Inverting 1)             [7] │ OUT2
           IN1+  │ [3] (Non-Inverting 1)         [6] │ IN2- (Inverting 2)
           GND   │ [4] (VCC- or GND)             [5] │ IN2+ (Non-Inverting 2)
                 └───────────────────────────────────┘
            """.trimIndent(),
            pinDetailsJson = "1: OUT1 (Amp 1 Output), 2: IN1- (Inverting Input 1), 3: IN1+ (Non-Inverting Input 1), 4: GND / V-, 5: IN2+ (Non-Inverting Input 2), 6: IN2- (Inverting Input 2), 7: OUT2 (Amp 2 Output), 8: VCC+ (3V-32V)",
            hackerTip = "Ground-sensing inputs allow input common-mode voltage down to 0V. Common in microphone preamplifiers, comparator circuits, audio detectors, and photodiode amplifiers.",
            antiHallucinationStatus = "VERIFIED",
            googleDork = "\"LM358\" datasheet (\"single supply\" | \"dual op amp\") filetype:pdf",
            datasheetUrl = "https://www.ti.com/lit/ds/symlink/lm358.pdf",
            suggestedDatasheetUrl = "https://www.ti.com/lit/ds/symlink/lm358.pdf"
        ),
        PinoutItem(
            id = "ams1117-33",
            name = "AMS1117-3.3 1A Low Dropout Regulator",
            chipCode = "AMS1117 / 1117-3.3",
            packageType = "SOT-223 / TO-252",
            voltageRange = "Vin: 4.75V - 12.0V, Vout: 3.3V Fixed (1.0A max)",
            description = "High efficiency positive low dropout (LDO) voltage regulator ubiquitous on NodeMCU, Arduino, and router PCB power rails.",
            pinDiagram = """
                 ┌────── AMS1117 (SOT-223) ──────┐
                 │          [TAB / PIN 4: VOUT]   │
                 │   [1]           [2]       [3]  │
                 │  GND/ADJ       VOUT       VIN  │
                 └────────────────────────────────┘
            """.trimIndent(),
            pinDetailsJson = "1: GND (or ADJ for adjustable version), 2: VOUT (+3.3V Output), 3: VIN (Input Supply up to 12V), TAB (Connected internally to Pin 2 VOUT for heat dissipation)",
            hackerTip = "WARNING: SOT-223 pinout is NOT the same as 7805! Pin 1 is GND, Pin 2/Tab is VOUT, Pin 3 is VIN. Requires minimum 10μF tantalum or 22μF ceramic on output to prevent high-frequency oscillation.",
            antiHallucinationStatus = "VERIFIED",
            googleDork = "\"AMS1117\" datasheet (\"3.3V\" | \"SOT-223\") filetype:pdf",
            datasheetUrl = "http://www.advanced-monolithic.com/pdf/ds1117.pdf",
            suggestedDatasheetUrl = "http://www.advanced-monolithic.com/pdf/ds1117.pdf"
        ),
        PinoutItem(
            id = "atmega328p",
            name = "ATmega328P 8-Bit AVR Microcontroller",
            chipCode = "ATmega328P / ATmega328",
            packageType = "DIP-28 / TQFP-32",
            voltageRange = "1.8V - 5.5V (16MHz @ 5V, 8MHz @ 3.3V)",
            description = "The classic AVR 8-bit RISC microcontroller powering Arduino Uno, Nano, and millions of embedded sensor devices.",
            pinDiagram = """
                 ┌────────── ATmega328P (DIP-28) ─────────┐
          RESET  │ [ 1] PC6                          [28] │ PC5 (A5 / SCL)
          RX     │ [ 2] PD0                          [27] │ PC4 (A4 / SDA)
          TX     │ [ 3] PD1                          [26] │ PC3 (A3)
          INT0   │ [ 4] PD2                          [25] │ PC2 (A2)
          INT1   │ [ 5] PD3 (PWM)                    [24] │ PC1 (A1)
          XCK    │ [ 6] PD4                          [23] │ PC0 (A0)
          VCC    │ [ 7] (+5V)                        [22] │ GND
          GND    │ [ 8]                              [21] │ AREF
          XTAL1  │ [ 9] PB6                          [20] │ AVCC (+5V)
          XTAL2  │ [10] PB7                          [19] │ PB5 (D13 / SCK)
          T1     │ [11] PD5 (PWM)                    [18] │ PB4 (D12 / MISO)
          AIN0   │ [12] PD6 (PWM)                    [17] │ PB3 (D11 / MOSI)
          AIN1   │ [13] PD7                          [16] │ PB2 (D10 / SS)
          ICP1   │ [14] PB0                          [15] │ PB1 (D9 / PWM)
                 └────────────────────────────────────────┘
            """.trimIndent(),
            pinDetailsJson = "1: PC6 RESET (Pull-up with 10k), 2: PD0 (RXD), 3: PD1 (TXD), 7: VCC, 8: GND, 9 & 10: 16MHz Crystal, 17: MOSI, 18: MISO, 19: SCK (D13), 20: AVCC, 22: GND, 27: A4 (I2C SDA), 28: A5 (I2C SCL)",
            hackerTip = "To salvage and reuse bare DIP-28 chips, program them using Arduino as ISP or a USBasp programmer connected to MISO (18), MOSI (17), SCK (19), RESET (1), VCC (7), GND (8).",
            antiHallucinationStatus = "VERIFIED",
            googleDork = "\"ATmega328P\" datasheet \"pinout\" filetype:pdf",
            datasheetUrl = "https://ww1.microchip.com/downloads/en/DeviceDoc/Atmel-7810-Automotive-Microcontrollers-ATmega328P_Datasheet.pdf",
            suggestedDatasheetUrl = "https://ww1.microchip.com/downloads/en/DeviceDoc/Atmel-7810-Automotive-Microcontrollers-ATmega328P_Datasheet.pdf"
        ),
        PinoutItem(
            id = "7805",
            name = "LM7805 / L7805 5V 1.5A Linear Regulator",
            chipCode = "7805 / L7805CV",
            packageType = "TO-220 / D2PAK",
            voltageRange = "Vin: 7.0V - 25.0V, Vout: 5.0V Fixed (1.5A max with heatsink)",
            description = "Bulletproof three-terminal positive voltage regulator with internal thermal overload and short-circuit protection.",
            pinDiagram = """
                 ┌─────────── 7805 (TO-220) ───────────┐
                 │       [HEATSINK TAB: GROUND]        │
                 │                                     │
                 │        [1]          [2]        [3]  │
                 │       INPUT        GROUND     OUTPUT│
                 └─────────────────────────────────────┘
            """.trimIndent(),
            pinDetailsJson = "Pin 1: INPUT (7V - 25V unregulated DC), Pin 2: GROUND (0V), Pin 3: OUTPUT (Regulated +5.0V), TAB: GROUND (Internally connected to Pin 2)",
            hackerTip = "Dropout voltage is ~2.0V, so input MUST be at least 7.0V. Add 0.33μF capacitor on input and 0.1μF on output right at pins to prevent thermal drift and RF oscillations.",
            antiHallucinationStatus = "VERIFIED",
            googleDork = "\"7805\" datasheet (\"TO-220\" | \"positive regulator\") filetype:pdf",
            datasheetUrl = "https://www.st.com/resource/en/datasheet/l78.pdf",
            suggestedDatasheetUrl = "https://www.st.com/resource/en/datasheet/l78.pdf"
        ),
        PinoutItem(
            id = "pc817",
            name = "PC817 / EL817 Optocoupler",
            chipCode = "PC817 / PC817C / EL817",
            packageType = "DIP-4 / SOP-4",
            voltageRange = "Viso: 5000Vrms, Vceo: 35V, If: 20mA (typ)",
            description = "High isolation voltage photocoupler containing an infrared emitting diode optically coupled to a phototransistor. Present on virtually every salvaged SMPS power supply.",
            pinDiagram = """
                 ┌────────── PC817 (DIP-4) ──────────┐
          ANODE  │ [1]                           [4] │ COLLECTOR
        CATHODE  │ [2]                           [3] │ EMITTER
                 └───────────────────────────────────┘
                       [DOT: PIN 1 ANODE]
            """.trimIndent(),
            pinDetailsJson = "1: ANODE (LED Input +), 2: CATHODE (LED Input -), 3: EMITTER (Output Phototransistor -), 4: COLLECTOR (Output Phototransistor +)",
            hackerTip = "The dot on the package indicates PIN 1 (Anode). Drive pin 1 with 5V via a 220Ω-470Ω resistor. When LED lights up, collector (pin 4) pulls down to emitter (pin 3). Essential for safely isolating 230V mains from low-voltage microcontrollers.",
            antiHallucinationStatus = "VERIFIED",
            googleDork = "\"PC817\" datasheet \"photocoupler\" filetype:pdf",
            datasheetUrl = "https://www.sharp-world.com/products/device/lineup/data/pdf/datasheet/pc817xnnsz_e.pdf",
            suggestedDatasheetUrl = "https://www.sharp-world.com/products/device/lineup/data/pdf/datasheet/pc817xnnsz_e.pdf"
        ),
        PinoutItem(
            id = "uln2003a",
            name = "ULN2003A 7-Channel Darlington Driver",
            chipCode = "ULN2003A / ULN2003",
            packageType = "DIP-16 / SOP-16",
            voltageRange = "Vce: 50V max, Ic: 500mA per channel (Output Clamp Diodes built-in)",
            description = "High-voltage high-current Darlington transistor array with suppression diodes for driving inductive loads (stepper motors, relays, solenoids).",
            pinDiagram = """
                 ┌────────── ULN2003 (DIP-16) ─────────┐
             IN1 │ [ 1]                           [16] │ OUT1
             IN2 │ [ 2]                           [15] │ OUT2
             IN3 │ [ 3]                           [14] │ OUT3
             IN4 │ [ 4]                           [13] │ OUT4
             IN5 │ [ 5]                           [12] │ OUT5
             IN6 │ [ 6]                           [11] │ OUT6
             IN7 │ [ 7]                           [10] │ OUT7
             GND │ [ 8]                           [ 9] │ COM (Clamp Diodes / VCC)
                 └─────────────────────────────────────┘
            """.trimIndent(),
            pinDetailsJson = "1-7: Logic Inputs (compatible with 3.3V and 5V TTL/CMOS), 8: GND (Emitters), 9: COM (Flyback diode common cathode - connect to load power supply!), 10-16: Open-Collector Outputs (sink current to GND)",
            hackerTip = "NEVER leave pin 9 (COM) floating when driving relays or 28BYJ-48 stepper motors! Connect Pin 9 directly to the motor/relay positive power rail (+5V or +12V) so internal clamp diodes absorb back-EMF spikes.",
            antiHallucinationStatus = "VERIFIED",
            googleDork = "\"ULN2003A\" datasheet \"darlington array\" filetype:pdf",
            datasheetUrl = "https://www.ti.com/lit/ds/symlink/uln2003a.pdf",
            suggestedDatasheetUrl = "https://www.ti.com/lit/ds/symlink/uln2003a.pdf"
        )
    )

    val INITIAL_PROJECTS = listOf(
        SalvageProject(
            title = "Pocket 18650 USB-C PD/5V Field Power Bank",
            status = "IN_PROGRESS",
            category = "EDC_POCKET_MODULE",
            description = "Build a compact field power pack from salvaged laptop 18650 cells, TP4056 charger, and 5V boost converter in a 3D-printed or mint tin shell.",
            requiredParts = "2x 18650 Cells, 1x TP4056 with Protection, 1x MT3608 or 5V Boost, 1x USB-C Receptacle, 1x 0.96\" OLED",
            difficulty = "BEGINNER",
            stepByStep = "1. Test internal resistance of 18650 cells.\n2. Parallel solder cells using nickel strips and spot welder or quick low-temp solder.\n3. Connect B+ and B- to TP4056 battery pins.\n4. Wire OUT+ and OUT- to boost converter.\n5. Calibrate output trimmer to exactly 5.10V.\n6. Add 5.1kΩ pull-downs on USB-C CC pins.\n7. Mount in insulated chassis.",
            matrixIndex = 21
        ),
        SalvageProject(
            title = "Wi-Fi Wardriving & Deauther Dongle",
            status = "IDEA",
            category = "HARDCORE_IOT_RF",
            description = "Salvaged ESP32 microcontroller with SSD1306 OLED and Li-Ion power running 802.11 packet analyzer and network audit firmware.",
            requiredParts = "1x ESP32-WROOM, 1x SSD1306 OLED, 1x 500mAh LiPo or 18650, 1x Micro-SD SPI Breakout, 1x Tactile Switch",
            difficulty = "INTERMEDIATE",
            stepByStep = "1. Flash ESP32 with Open Source Wardriving / Deauther firmware.\n2. Wire I2C OLED (SDA->IO21, SCL->IO22).\n3. Wire Micro-SD card reader to SPI (MOSI->23, MISO->19, SCK->18, CS->5).\n4. Add navigation tactile button to IO0.\n5. Test GPS NMEA parsing over hardware UART2.",
            matrixIndex = 2
        ),
        SalvageProject(
            title = "LoRa Mesh Off-Grid Field Repeater",
            status = "TESTING",
            category = "HARDCORE_IOT_RF",
            description = "Solar-powered autonomous packet radio node for disaster preparedness and encrypted peer-to-peer mesh networking.",
            requiredParts = "1x ESP32 or nRF52, 1x SX1276 LoRa module, 1x Small 5V Solar Cell, 1x TP4056, 1x 18650 Cell, IP67 Waterproof box",
            difficulty = "EXPERT",
            stepByStep = "1. Connect SX1276 SPI bus to ESP32.\n2. Solder quarter-wave whip antenna or coaxial SMA connector.\n3. Install Meshtastic or DisasterMesh firmware.\n4. Hook solar panel to TP4056 Vin.\n5. Program low-power deep sleep cycles between beacon transmissions.",
            matrixIndex = 1
        ),
        SalvageProject(
            title = "Pocket Logic Probe with Audio Clicker",
            status = "DONE",
            category = "EDC_POCKET_MODULE",
            description = "Indispensable workshop troubleshooting pen using a salvaged NE555 timer and dual bi-color LEDs to detect HIGH, LOW, and high-frequency PWM pulses.",
            requiredParts = "1x NE555 IC, 1x Red/Green Dual LED, 1x Piezo Buzzer, 3x 1kΩ Resistors, 1x Probe Needle, Syringe/Pen Case",
            difficulty = "BEGINNER",
            stepByStep = "1. Configure NE555 in window comparator mode.\n2. Wire probe tip to trigger/threshold.\n3. Red LED lights on >2.4V (Logic HIGH).\n4. Green LED lights on <0.8V (Logic LOW).\n5. Piezo audio clicker ticks on toggling digital clocks up to 100kHz.",
            matrixIndex = 23
        )
    )

    // THE 35-POINT EDC & REPURPOSE BRAINSTORMING MATRIX
    // 20 Hardcore IoT/RF + 15 EDC Pocket Modules
    val REPURPOSE_MATRIX: List<RepurposeMatrixItem> = listOf(
        // 20 Hardcore IoT/RF
        RepurposeMatrixItem(
            index = 1,
            title = "LoRa Mesh Field Repeater",
            category = "Hardcore IoT/RF",
            salvageSources = "Broken outdoor security cam, router box, garden solar",
            keySalvagedChips = "ESP32, SX1276 / LoRa, 18650 cell, TP4056",
            difficulty = "HACKER-OPS",
            description = "Autonomous off-grid solar repeater running Meshtastic. Relays encrypted emergency messages over 10-15km radius without cellular or internet infrastructure.",
            pinoutHint = "SPI: MOSI-IO23, MISO-IO19, SCK-IO18, CS-IO5, RST-IO14, DIO0-IO2"
        ),
        RepurposeMatrixItem(
            index = 2,
            title = "Wi-Fi Wardriving & Deauther Dongle",
            category = "Hardcore IoT/RF",
            salvageSources = "Discarded smart home plugs, Sonoff modules, e-bike screens",
            keySalvagedChips = "ESP8266 / ESP32, SSD1306 OLED, salvaged push buttons",
            difficulty = "INTERMEDIATE",
            description = "Pocket packet sniffer and network audit appliance. Logs PCAP Wi-Fi handshakes to SD card for penetration testing and rogue AP detection.",
            pinoutHint = "Wire OLED to IO21(SDA) & IO22(SCL); Use IO0 as mode toggle button"
        ),
        RepurposeMatrixItem(
            index = 3,
            title = "Off-Grid APRS Packet Radio Digipeater",
            category = "Hardcore IoT/RF",
            salvageSources = "Scrapped Baofeng walkie-talkie, broken audio headset",
            keySalvagedChips = "ATmega328P, LM386 Audio Amp, optocoupler 4N35 for PTT",
            difficulty = "EXPERT",
            description = "Transmits automated emergency position packets and telemetry bursts over 144.800MHz amateur radio spectrum without cellular grid.",
            pinoutHint = "Optocoupler pin 1/2 to MCU GPIO via 330Ω, pin 4/5 across radio PTT line"
        ),
        RepurposeMatrixItem(
            index = 4,
            title = "2.4GHz RF Spectrum Sweeper & Bug Detector",
            category = "Hardcore IoT/RF",
            salvageSources = "Toy quadcopter remote controller, scrap wireless mouse",
            keySalvagedChips = "nRF24L01+, Arduino Nano / ESP32, bargraph LED display",
            difficulty = "INTERMEDIATE",
            description = "Rapidly hops across all 128 channels in 2.4GHz band to measure RSSI energy. Detects hidden covert wireless cameras, rogue drones, and wireless transmitters.",
            pinoutHint = "nRF24: CE-IO9, CSN-IO10, SCK-IO13, MOSI-IO11, MISO-IO12"
        ),
        RepurposeMatrixItem(
            index = 5,
            title = "Autonomous Solar Weather Telemetry Node",
            category = "Hardcore IoT/RF",
            salvageSources = "Broken garden lawn solar lights, scrap weather station",
            keySalvagedChips = "ESP32 deep sleep mode, BMP280, salvaged 5.5V supercapacitor",
            difficulty = "INTERMEDIATE",
            description = "Ultra-low-power environmental sensor transmitting barometric pressure, temperature, and humidity every 15 minutes over ESP-NOW mesh.",
            pinoutHint = "BMP280 I2C 0x76: VCC-3.3V, GND, SCL, SDA; Use deep sleep RTC timer"
        ),
        RepurposeMatrixItem(
            index = 6,
            title = "Reverse-Engineered Smart Plug Power Logger",
            category = "Hardcore IoT/RF",
            salvageSources = "Tuya / Sonoff smart plugs with failed relays",
            keySalvagedChips = "ESP8266 (ESP8285), BL0937 / HLW8012 power meter IC",
            difficulty = "HACKER-OPS",
            description = "Flash open-source Tasmota or ESPHome via serial pins to log high-voltage AC mains power consumption, voltage dips, and wattage without vendor cloud.",
            pinoutHint = "HIGH VOLTAGE SAFETY: Flash ONLY while disconnected from AC mains via 3.3V UART!"
        ),
        RepurposeMatrixItem(
            index = 7,
            title = "IR Blaster & Remote Jammer Ninja",
            category = "Hardcore IoT/RF",
            salvageSources = "Old CRT TV remotes, broken DVD players, scrap camcorders",
            keySalvagedChips = "High-power 940nm IR LEDs, 2N2222 NPN transistor, ATTiny85",
            difficulty = "BEGINNER",
            description = "Stores universal power-off hex codes to shut down distracting public televisions or trigger custom home automation macros from a keyfob.",
            pinoutHint = "Base to GPIO via 1kΩ, Emitter to GND, Collector through IR LED + 22Ω to 5V"
        ),
        RepurposeMatrixItem(
            index = 8,
            title = "SDR Antenna Low-Noise Preamplifier (LNA)",
            category = "Hardcore IoT/RF",
            salvageSources = "Discarded TV tuner boards, broken satellite LNB dish feeds",
            keySalvagedChips = "BF998 dual-gate MOSFET or BFP420 RF transistor, SMA connector",
            difficulty = "EXPERT",
            description = "Boosts weak VHF/UHF radio signals for RTL-SDR software-defined radio receivers to track aviation ADS-B and NOAA weather satellites.",
            pinoutHint = "Keep RF trace lengths < 5mm with 50Ω ground plane clearance"
        ),
        RepurposeMatrixItem(
            index = 9,
            title = "GSM/SMS Silent Perimeter Tripwire",
            category = "Hardcore IoT/RF",
            salvageSources = "Old 2G/4G cellular GPS tracker, broken vehicle alarm",
            keySalvagedChips = "SIM800L / A6 module, PIR motion sensor, 18650 cell",
            difficulty = "INTERMEDIATE",
            description = "Sends instant encrypted SMS alert to your phone when an optical or magnetic tripwire is breached in workshop or cabin.",
            pinoutHint = "SIM800L requires 3.8V-4.2V and 2A pulse current. Add 1000uF low-ESR capacitor!"
        ),
        RepurposeMatrixItem(
            index = 10,
            title = "CAN-Bus Sniffer & Vehicle Telemetry Dongle",
            category = "Hardcore IoT/RF",
            salvageSources = "Scrapped automotive dashboard cluster, broken ECU module",
            keySalvagedChips = "MCP2515 CAN controller, TJA1050 transceiver, ESP32",
            difficulty = "EXPERT",
            description = "Hooks into OBD2 diagnostic port to sniff high-speed 500kbps CAN messages, decode engine RPM, speed, and reverse-engineer door lock commands.",
            pinoutHint = "SPI connection to MCP2515; Terminate CANH and CANL with 120Ω resistor"
        ),
        RepurposeMatrixItem(
            index = 11,
            title = "5.8GHz FPV Video Ground Station Monitor",
            category = "Hardcore IoT/RF",
            salvageSources = "Crashed drone receiver, portable DVD player LCD screen",
            keySalvagedChips = "RX5808 video receiver module, SPI channel mod",
            difficulty = "INTERMEDIATE",
            description = "Scans all 48 channels in the 5.8GHz video band and displays incoming analog video feeds on a portable salvaged display.",
            pinoutHint = "Ground pins CH1, CH2, CH3 on RX5808 to enable SPI control"
        ),
        RepurposeMatrixItem(
            index = 12,
            title = "BLE Beacon Spoofer & Proximity Trigger",
            category = "Hardcore IoT/RF",
            salvageSources = "Smart fitness bands with broken screens, Tile tracker",
            keySalvagedChips = "nRF51822 / nRF52832 or ESP32-C3, coin cell holder",
            difficulty = "BEGINNER",
            description = "Broadcasts iBeacon or Eddystone packets to unlock doors or launch Home Assistant automation routines when your key approaches.",
            pinoutHint = "Configure advertising interval to 1000ms for 18-month battery life on CR2032"
        ),
        RepurposeMatrixItem(
            index = 13,
            title = "Zigbee Home Assistant Mesh Extender",
            category = "Hardcore IoT/RF",
            salvageSources = "Discarded Philips Hue / Ikea Trådfri smart bulbs with dead LEDs",
            keySalvagedChips = "CC2530 / EFR32MG Zigbee SoC module, 3.3V regulator",
            difficulty = "INTERMEDIATE",
            description = "Salvage the functioning wireless module from burned-out LED lightbulbs to extend Zigbee mesh coverage across concrete workshop walls.",
            pinoutHint = "Flash Zigbee router firmware via CC Debugger or Raspberry Pi GPIO"
        ),
        RepurposeMatrixItem(
            index = 14,
            title = "Ultrasonic Directional Acoustic Rig",
            category = "Hardcore IoT/RF",
            salvageSources = "Automotive reverse parking sensors, scrap air humidifiers",
            keySalvagedChips = "40kHz ultrasonic piezoceramic transducers, push-pull driver",
            difficulty = "EXPERT",
            description = "Generates high-frequency 40kHz ultrasound beam for directional parametric audio projection or acoustic levitation experiments.",
            pinoutHint = "Drive transducers with H-bridge or TC4427 MOSFET gate driver at resonance"
        ),
        RepurposeMatrixItem(
            index = 15,
            title = "Thermal Camera Breakout Rig",
            category = "Hardcore IoT/RF",
            salvageSources = "Defective Caterpillar rugged phones, inspection scopes",
            keySalvagedChips = "FLIR Lepton core or MLX90640, SPI interface, SPI LCD",
            difficulty = "HACKER-OPS",
            description = "Salvage thermal microbolometer cores to visualize PCB thermal hotspots, short circuits, and heat leaks in housing.",
            pinoutHint = "Lepton uses VoSPI (Video over SPI) + I2C command interface at 3.3V"
        ),
        RepurposeMatrixItem(
            index = 16,
            title = "Galvanic Skin Response (GSR) Bio-Sensor",
            category = "Hardcore IoT/RF",
            salvageSources = "Broken pulse oximeter, scrap medical electrodes",
            keySalvagedChips = "LM358 op-amp in instrumentation mode, ADC input",
            difficulty = "BEGINNER",
            description = "Measures subtle skin resistance variations caused by sympathetic nervous system activation for biofeedback training.",
            pinoutHint = "High-impedance voltage divider with 100kΩ reference resistor"
        ),
        RepurposeMatrixItem(
            index = 17,
            title = "Optical Laser Audio Transceiver",
            category = "Hardcore IoT/RF",
            salvageSources = "Scrap DVD burner optical pickup, solar calculator cell",
            keySalvagedChips = "650nm Red laser diode, LM386 audio amplifier",
            difficulty = "BEGINNER",
            description = "Transmits analog voice audio over a collimated laser beam up to 500 meters to an optical receiver with zero RF emission.",
            pinoutHint = "Modulate laser current through audio transformer or small transistor"
        ),
        RepurposeMatrixItem(
            index = 18,
            title = "Sub-1GHz Rolling Code Capture Rig",
            category = "Hardcore IoT/RF",
            salvageSources = "Garage door opener remote, wireless doorbell receiver",
            keySalvagedChips = "CC1101 transceiver, STM32F103 / ESP32",
            difficulty = "HACKER-OPS",
            description = "Captures raw OOK / FSK RF packets on 433.92MHz and 868MHz for RF replay audits and smart meter telemetry analysis.",
            pinoutHint = "CC1101 SPI bus with GDO0 configured for digital sync-word interrupt"
        ),
        RepurposeMatrixItem(
            index = 19,
            title = "RFID / NFC Badge Duplicator & Pentester",
            category = "Hardcore IoT/RF",
            salvageSources = "Broken door entry panel, old hotel card lock",
            keySalvagedChips = "RC522 (13.56MHz) + EM4100 (125kHz) coils, OLED",
            difficulty = "INTERMEDIATE",
            description = "Reads, clones, and emulates proximity entry cards and keyfobs for access control audits.",
            pinoutHint = "RC522 SPI pins are 3.3V ONLY. 5V logic inputs will destroy receiver!"
        ),
        RepurposeMatrixItem(
            index = 20,
            title = "Isolated High-Voltage Flyback Plasma Arc",
            category = "Hardcore IoT/RF",
            salvageSources = "CRT monitor flyback transformer, CCFL laptop backlight inverter",
            keySalvagedChips = "IRFZ44N / IRF3205 power MOSFET, NE555 oscillator",
            difficulty = "HACKER-OPS",
            description = "Generates 15kV electric arcs for plasma speaker experiments, ozone synthesis, and spark testing.",
            pinoutHint = "DANGER: HIGH VOLTAGE. Install fast flyback snubber diode and large heatsink!"
        ),

        // 15 EDC Pocket Modules
        RepurposeMatrixItem(
            index = 21,
            title = "Pocket USB-C Fast Charger & 18650 Bank",
            category = "EDC Pocket Module",
            salvageSources = "Old Dell / HP laptop batteries, disposable vape cells",
            keySalvagedChips = "TP4056 + MT3608 or IP5306 all-in-one power SoC",
            difficulty = "BEGINNER",
            description = "Pocket-sized reliable emergency phone charger with CC/CV protection, USB-C in/out, and 18650 battery holder.",
            pinoutHint = "Solder 5.1kΩ pull-downs on CC1 & CC2 pins of the USB-C receptacle"
        ),
        RepurposeMatrixItem(
            index = 22,
            title = "BadUSB Keystroke Injector",
            category = "EDC Pocket Module",
            salvageSources = "Defective mechanical gaming keyboards, Arduino Leonardo",
            keySalvagedChips = "ATmega32U4 / CH552 / RP2040, Micro-SD slot",
            difficulty = "INTERMEDIATE",
            description = "Presents itself as a trusted USB HID keyboard and executes automated sysadmin configurations and security audit scripts at 1000 WPM.",
            pinoutHint = "Connect USB D- to pin D-, D+ to pin D+ with 22Ω series dampening resistors"
        ),
        RepurposeMatrixItem(
            index = 23,
            title = "Pocket Logic Probe with Audio Clicker",
            category = "EDC Pocket Module",
            salvageSources = "Discarded PC motherboard, scrap toys",
            keySalvagedChips = "NE555 timer, red/green LED, miniature piezo buzzer",
            difficulty = "BEGINNER",
            description = "Pen-sized diagnostic tool that tests TTL / CMOS digital circuits, detects logic high, low, floating states, and oscillation pulses.",
            pinoutHint = "Use 1MΩ input impedance resistor to avoid loading sensitive circuits"
        ),
        RepurposeMatrixItem(
            index = 24,
            title = "Continuous Low-Resistance Continuity Buzzer",
            category = "EDC Pocket Module",
            salvageSources = "Smoke detector buzzer, dead multimeter",
            keySalvagedChips = "LM393 dual comparator, precision 0.1Ω threshold reference",
            difficulty = "BEGINNER",
            description = "Ultra-fast continuity tester that beeps immediately (<1ms) without the annoying delay of cheap multimeters. Tests solder joints and detects PCB shorts.",
            pinoutHint = "Non-inverting comparator with 50mV trip voltage for genuine <1Ω beeps"
        ),
        RepurposeMatrixItem(
            index = 25,
            title = "USB Power Delivery (PD) Trigger Decoy",
            category = "EDC Pocket Module",
            salvageSources = "Broken USB-C laptop docking stations",
            keySalvagedChips = "CH224K / IP2721 / FUSB302 PD sink controller, DIP switch",
            difficulty = "INTERMEDIATE",
            description = "Negotiates 9V, 12V, 15V, or 20V from any modern USB-C GaN charger to power soldering irons, routers, and ham radios in the field.",
            pinoutHint = "CH224K pins CFG1, CFG2, CFG3 configured with pull-down resistors for voltage"
        ),
        RepurposeMatrixItem(
            index = 26,
            title = "Mini PWM Fan & Motor Speed Dial",
            category = "EDC Pocket Module",
            salvageSources = "PC cooling fans, scrap hoverboard throttle, printer motors",
            keySalvagedChips = "NE555 / ATTiny85, 10k potentiometer, IRFZ44N MOSFET",
            difficulty = "BEGINNER",
            description = "Pocket pulse-width controller to adjust cooling fans, dim high-power LED strips, and test DC motors smoothly from 0% to 100%.",
            pinoutHint = "NE555 astable oscillator with dual 1N4148 diodes across potentiometer"
        ),
        RepurposeMatrixItem(
            index = 27,
            title = "Pocket Component Tester & ESR Meter",
            category = "EDC Pocket Module",
            salvageSources = "Scrap multimeter, broken calculators, old Arduino Uno",
            keySalvagedChips = "ATmega328P, 128x64 LCD or OLED, precision 680Ω & 470kΩ resistors",
            difficulty = "INTERMEDIATE",
            description = "Open-source transistor and capacitor tester in a pocket mint tin. Identifies unknown salvaged transistors, pinout, hFE, and capacitor ESR.",
            pinoutHint = "Requires 0.1% tolerance resistors on test pins 1, 2, 3 for high accuracy"
        ),
        RepurposeMatrixItem(
            index = 28,
            title = "High-CRI Workshop Inspection Penlight",
            category = "EDC Pocket Module",
            salvageSources = "High-end dead television backlights, broken monitor panels",
            keySalvagedChips = "High-CRI 95+ 5000K SMD LEDs, AMC7135 constant current regulator",
            difficulty = "BEGINNER",
            description = "Pocket inspection torch revealing true component colors, burnt resistor bands, and micro solder bridges without color distortion.",
            pinoutHint = "AMC7135 regulates current to exact 350mA without series power resistors"
        ),
        RepurposeMatrixItem(
            index = 29,
            title = "Ultrasonic & White Noise Speech Masker",
            category = "EDC Pocket Module",
            salvageSources = "Defective ultrasonic pest chaser, miniature earphone drivers",
            keySalvagedChips = "NE555 / pseudo-random shift register (CD4006), audio amp",
            difficulty = "INTERMEDIATE",
            description = "Produces acoustic desynchronization and pink noise to protect confidential workshop discussions from phone microphone recording.",
            pinoutHint = "Output bandwidth tuned to voice band 300Hz - 3400Hz"
        ),
        RepurposeMatrixItem(
            index = 30,
            title = "USB Data Blocker (Pure Charge Condom)",
            category = "EDC Pocket Module",
            salvageSources = "Scrap USB extension cords, broken flash drives",
            keySalvagedChips = "USB-A male & female connectors, 0.91\" I2C OLED voltmeter",
            difficulty = "BEGINNER",
            description = "Physically cuts D+ and D- data pins so you can safely recharge your device at hostile public airports without risk of malware or Juice Jacking.",
            pinoutHint = "Pass only VBUS and GND lines; short D+ to D- on phone side for fast charge"
        ),
        RepurposeMatrixItem(
            index = 31,
            title = "18650 Internal Resistance Milliohm Tester",
            category = "EDC Pocket Module",
            salvageSources = "Broken vaping mod, precision power resistors",
            keySalvagedChips = "INA219 I2C current/voltage sensor, 5Ω 10W load resistor, OLED",
            difficulty = "INTERMEDIATE",
            description = "Performs AC/pulsed DC load drop test on salvaged lithium cells in under 2 seconds to instantly triage healthy cells from fire hazards.",
            pinoutHint = "Formula: R_internal = (V_open - V_load) / I_load; healthy cells < 45mΩ"
        ),
        RepurposeMatrixItem(
            index = 32,
            title = "Pocket Laser Distance & Alignment Rig",
            category = "EDC Pocket Module",
            salvageSources = "Scrap laser printer scanner, robotic vacuum cleaner LIDAR",
            keySalvagedChips = "VL53L0X / VL53L1X Time-of-Flight sensor, SSD1306 OLED",
            difficulty = "BEGINNER",
            description = "Millimeter-accurate optical distance finder up to 2 meters for workshop carpentry, robot navigation, and precise sensor alignment.",
            pinoutHint = "VL53L0X operates on I2C address 0x29 at 2.8V-3.3V"
        ),
        RepurposeMatrixItem(
            index = 33,
            title = "Micro Frequency Counter (1Hz - 50MHz)",
            category = "EDC Pocket Module",
            salvageSources = "Old FM radios, scrapped CRT monitors, motherboards",
            keySalvagedChips = "74HC390 decade counter prescaler, ATmega328P, 0.96\" OLED",
            difficulty = "INTERMEDIATE",
            description = "Measures clock oscillators, crystal health, PWM signals, and RF carrier frequencies directly in circuit.",
            pinoutHint = "Feed signal into Timer1 counter input pin (T1 / Pin 5 on ATmega328P)"
        ),
        RepurposeMatrixItem(
            index = 34,
            title = "Stealth Audio Spy & Bug Detector Pen",
            category = "EDC Pocket Module",
            salvageSources = "Old cellular phone microphone, broken MP3 player",
            keySalvagedChips = "MAX9814 auto-gain electret microphone amplifier, Micro-SD SPI",
            difficulty = "INTERMEDIATE",
            description = "Pocket ambient sound recorder with automatic gain control to capture faint audio or detect high-frequency coil whine.",
            pinoutHint = "MAX9814 GAIN pin: Unconnected = 60dB, GND = 50dB, VCC = 40dB"
        ),
        RepurposeMatrixItem(
            index = 35,
            title = "Pocket DDS Function Signal Generator",
            category = "EDC Pocket Module",
            salvageSources = "Scrapped telecom modem, audio mixer, synthesizer",
            keySalvagedChips = "AD9833 programmable waveform generator, rotary encoder",
            difficulty = "INTERMEDIATE",
            description = "Generates clean sine, triangle, and square waveforms from 0.1Hz to 12.5MHz with 0.1Hz resolution for bench audio and filter calibration.",
            pinoutHint = "AD9833 SPI: FSYNC-IO5, SCLK-IO18, SDATA-IO23; Output impedance ~200Ω"
        )
    )

    val INITIAL_RAG_DOCS = listOf(
        RagDocument(
            title = "TP4056_Datasheet_Summary.txt",
            sourceName = "NanJing Top Power ASIC Corp Spec Sheet Rev 2.4",
            content = """
            TP4056 is a complete constant-current/constant-voltage linear charger for single cell lithium-ion batteries.
            Key parameters:
            - Input Supply Voltage (VCC): 4.0V to 8.0V (absolute max 9.0V)
            - Target Battery Regulation Voltage: 4.2V ±1%
            - Charge Current: Programmable up to 1000mA via resistor Rprog from pin 2 to GND.
            - Current formula: I_bat = (V_prog / R_prog) * 1200 = (1.0V / R_prog) * 1200
            - Resistor table: 1.2kΩ = 1000mA, 1.66kΩ = 720mA, 2.0kΩ = 580mA, 3.0kΩ = 400mA, 10kΩ = 130mA.
            - Thermal regulation reduces current if die temp exceeds 145°C.
            - Pinout: 1-TEMP, 2-PROG, 3-GND, 4-VCC, 5-BAT, 6-STDBY, 7-CHRG, 8-CE.
            """.trimIndent(),
            category = "Datasheets"
        ),
        RagDocument(
            title = "18650_Lithium_Salvage_Safety_Protocol.md",
            sourceName = "HackerOps E-Waste Salvage & Battery Safety Manual v3.2",
            content = """
            Protocol for reclaiming 18650 cells from discarded laptop and tool batteries:
            1. Visual Inspection: Reject cells with rust on negative terminal, dented positive vents, or damaged shrink wrap.
            2. Voltage Check:
               - Cell voltage > 2.5V: Healthy candidate. Proceed to slow recharge.
               - Cell voltage 1.5V - 2.5V: Degraded. Charge at maximum 100mA until 3.0V.
               - Cell voltage < 1.0V: Danger of copper shunts / internal dendritic shorts. Recycle immediately.
            3. Internal Resistance (IR): Good power cell is < 35mΩ. Good energy cell is < 55mΩ. Over 80mΩ produces excessive heat under load.
            4. Capacity Testing: Charge to 4.20V, rest for 2 hours, discharge at 500mA down to 2.80V.
            """.trimIndent(),
            category = "Safety & Protocol"
        ),
        RagDocument(
            title = "ESP32_Boot_Pin_Strap_Table.txt",
            sourceName = "Espressif Hardware Design Guidelines ESP32-D0WD",
            content = """
            ESP32 Strapping Pins Reference for Salvage & Flashing:
            - GPIO0: Boot Mode Selection. Pulled HIGH internally. Pull LOW to GND during RESET to enter ROM Serial Download Mode for flashing.
            - GPIO2: Must be floating or LOW to enter bootloader. Connected to on-board LED on many modules.
            - GPIO12 (MTDI): Selects SPI flash LDO voltage. LOW = 3.3V flash, HIGH = 1.8V flash. Pulling this HIGH causes instant boot crash on 3.3V flash chips!
            - GPIO15: Must be HIGH during boot for normal silent boot output.
            - GPIO34, GPIO35, GPIO36 (VP), GPIO39 (VN): INPUT ONLY pins. Cannot be configured as outputs or with internal pull-up/pull-down resistors.
            """.trimIndent(),
            category = "Microcontrollers"
        )
    )
}
