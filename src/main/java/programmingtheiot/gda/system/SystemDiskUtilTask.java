package programmingtheiot.gda.system;

import java.io.File;
import programmingtheiot.common.ConfigConst;

/**
 * Shell representation of class for student implementation.
 *
 */
public class SystemDiskUtilTask extends BaseSystemUtilTask
{
    private String diskPath;

    public SystemDiskUtilTask(String diskPath)
    {
        super(ConfigConst.NOT_SET, ConfigConst.DEFAULT_TYPE_ID);
        this.diskPath = diskPath;
    }


    @Override
    public float getTelemetryValue()
    {
        File disk = new File(this.diskPath);

        if (!disk.exists() || !disk.isDirectory())
        {
            return 0;
         }

        long totalSpace = disk.getTotalSpace();
        long freeSpace = disk.getFreeSpace();

        long usedSpace = totalSpace - freeSpace;

        return (float) usedSpace / totalSpace * 100;
    }
}
