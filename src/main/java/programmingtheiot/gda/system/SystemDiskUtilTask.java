package programmingtheiot.gda.system;

import java.io.File;
import programmingtheiot.common.ConfigConst;
import java.util.logging.Logger;
/**
 * Shell representation of class for student implementation.
 *
 */
public class SystemDiskUtilTask extends BaseSystemUtilTask
{
    private String diskPath;

    // Existing constructor
    public SystemDiskUtilTask(String diskPath)
    {
        super(ConfigConst.NOT_SET, ConfigConst.DEFAULT_TYPE_ID);
        this.diskPath = diskPath;
    }

    // ✅ Add a default constructor
    public SystemDiskUtilTask()
    {
        super(ConfigConst.NOT_SET, ConfigConst.DEFAULT_TYPE_ID);
        this.diskPath = "C:\\"; // Set a default disk path (modify as needed)
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
