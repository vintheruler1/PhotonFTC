package com.outoftheboxrobotics.photoncore.hardware;

import com.outoftheboxrobotics.photoncore.PhotonLastKnown;
import com.outoftheboxrobotics.photoncore.hardware.motor.commands.PhotonLynxI2cReadMultipleBytesCommand;
import com.qualcomm.hardware.lynx.LynxModule;
import com.qualcomm.hardware.lynx.LynxNackException;
import com.qualcomm.hardware.lynx.LynxUnsupportedCommandException;
import com.qualcomm.robotcore.hardware.I2cAddr;
import com.qualcomm.robotcore.hardware.I2cDeviceSynch;
import com.qualcomm.robotcore.hardware.I2cDeviceSynchDevice;
import com.qualcomm.robotcore.hardware.NormalizedRGBA;
import com.qualcomm.robotcore.util.TypeConversion;
import java.util.concurrent.CompletableFuture;

/**
 * Optimized LynxColorSensor. Allows for asynchronous reads of color values.
 * This implementation is based on the REV Color Sensor V2 (APDS-9960).
 *
 * Not written by me but @mdad1427 (probably ai ngl)
 * TODO: Redo.
 */
public class PhotonLynxColorSensor extends I2cDeviceSynchDevice<I2cDeviceSynch> implements com.qualcomm.robotcore.hardware.ColorSensor {

    public static final I2cAddr ADDRESS_I2C_DEFAULT = I2cAddr.create7bit(0x39);

    // Registers for the Avago APDS-9960, used in the REV Color Sensor V2
    public enum Register {
        ENABLE(0x80),
        ATIME(0x81),
        WTIME(0x83),
        AILTL(0x84),
        AILTH(0x85),
        AIHTL(0x86),
        AIHTH(0x87),
        PILT(0x89),
        PIHT(0x8B),
        PERS(0x8C),
        CONFIG1(0x8D),
        PPULSE(0x8E),
        CONTROL(0x8F),
        CONFIG2(0x90),
        ID(0x92),
        STATUS(0x93),
        CDATAL(0x94),
        CDATAH(0x95),
        RDATAL(0x96),
        RDATAH(0x97),
        GDATAL(0x98),
        GDATAH(0x99),
        BDATAL(0x9A),
        BDATAH(0x9B),
        PDATA(0x9C),
        POFFSET_UR(0x9D),
        POFFSET_DL(0x9E),
        CONFIG3(0x9F),
        UNKNOWN(0);

        public final byte bVal;
        Register(int i) { this.bVal = (byte) i; }
    }

    private final PhotonLastKnown<NormalizedRGBA> lastKnownRgba = new PhotonLastKnown<>(false);

    public PhotonLynxColorSensor(I2cDeviceSynch deviceClient) {
        super(deviceClient, true);
        this.deviceClient.setI2cAddress(ADDRESS_I2C_DEFAULT);
        super.registerArmingStateCallback(false);
        this.deviceClient.engage();
    }

    @Override
    protected boolean doInitialize() {
        // Enable the sensor
        // Power On (PON) and ADC Enable (AEN)
        byte enableCommand = 0x03;
        deviceClient.write8(Register.ENABLE.bVal, enableCommand);
        return true;
    }

    @Override
    public Manufacturer getManufacturer() {
        return Manufacturer.Broadcom;
    }

    @Override
    public String getDeviceName() {
        return "Photon REV Color Sensor V2";
    }

    @Override
    public int red() {
        return getNormalizedColors().red;
    }

    @Override
    public int green() {
        return getNormalizedColors().green;
    }

    @Override
    public int blue() {
        return getNormalizedColors().blue;
    }

    @Override
    public int alpha() {
        return getNormalizedColors().alpha;
    }

    @Override
    public int argb() {
        return getNormalizedColors().toColor();
    }

    @Override
    public NormalizedRGBA getNormalizedColors() {
        // This is a blocking call for compatibility.
        // It reads 8 bytes starting from the clear data low byte register.
        byte[] readData = deviceClient.read(Register.CDATAL.bVal, 8);

        // Data is in order: Clear, Red, Green, Blue
        int alpha = TypeConversion.unsignedShortToInt(TypeConversion.combineLoHi(readData[0], readData[1])); // Clear
        int red = TypeConversion.unsignedShortToInt(TypeConversion.combineLoHi(readData[2], readData[3]));
        int green = TypeConversion.unsignedShortToInt(TypeConversion.combineLoHi(readData[4], readData[5]));
        int blue = TypeConversion.unsignedShortToInt(TypeConversion.combineLoHi(readData[6], readData[7]));

        NormalizedRGBA rgba = new NormalizedRGBA();
        rgba.red = red;
        rgba.green = green;
        rgba.blue = blue;
        rgba.alpha = alpha; // Alpha is the clear channel reading

        lastKnownRgba.setValue(rgba);
        return rgba;
    }

    /**
     * Get the cached RGBA values.
     * @return The last known RGBA values.
     */
    public NormalizedRGBA getCachedNormalizedColors() {
        return lastKnownRgba.getValue();
    }

    /**
     * Asynchronously reads the color sensor data.
     * @return A CompletableFuture that will be completed with the RGBA values.
     */
    public CompletableFuture<NormalizedRGBA> getNormalizedColorsAsync() {
        PhotonLynxI2cReadMultipleBytesCommand command = new PhotonLynxI2cReadMultipleBytesCommand(
                (LynxModule) this.deviceClient.getModule(),
                this.deviceClient.getI2cAddr().get7Bit(),
                Register.CDATAL.bVal,
                8
        );

        try {
            command.acquireNetworkLock();
            ((LynxModule) this.deviceClient.getModule()).sendCommand(command);
            return command.getResponse().thenApply((message) -> {
                try {
                    command.releaseNetworkLock();
                } catch (InterruptedException e) {
                    handleException(e);
                }
                byte[] readData = (byte[]) message.getPayloadData();

                // Data is in order: Clear, Red, Green, Blue
                int alpha = TypeConversion.unsignedShortToInt(TypeConversion.combineLoHi(readData[0], readData[1])); // Clear
                int red = TypeConversion.unsignedShortToInt(TypeConversion.combineLoHi(readData[2], readData[3]));
                int green = TypeConversion.unsignedShortToInt(TypeConversion.combineLoHi(readData[4], readData[5]));
                int blue = TypeConversion.unsignedShortToInt(TypeConversion.combineLoHi(readData[6], readData[7]));

                NormalizedRGBA rgba = new NormalizedRGBA();
                rgba.red = red;
                rgba.green = green;
                rgba.blue = blue;
                rgba.alpha = alpha; // Alpha is the clear channel reading

                lastKnownRgba.setValue(rgba);
                return rgba;
            });
        } catch (InterruptedException | RuntimeException | LynxUnsupportedCommandException | LynxNackException e) {
            handleException(e);
        }
        return CompletableFuture.completedFuture(new NormalizedRGBA()); // Return a default value on failure
    }

    @Override
    public void enableLed(boolean enable) {
        // Not implemented for this optimized version to keep it simple
    }

    @Override
    public void setI2cAddress(I2cAddr newAddress) {
        deviceClient.setI2cAddress(newAddress);
    }

    @Override
    public I2cAddr getI2cAddress() {
        return deviceClient.getI2cAddress();
    }
}

