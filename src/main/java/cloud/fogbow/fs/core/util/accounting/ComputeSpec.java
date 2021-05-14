package cloud.fogbow.fs.core.util.accounting;

public class ComputeSpec extends OrderSpec {

    private int vCpu;
    private int ram;

    public ComputeSpec(int vCpu, int ram) {
        this.vCpu = vCpu;
        this.ram = ram;
    }

    public ComputeSpec() {}

    public int getvCpu() {
        return vCpu;
    }

    public void setvCpu(int vCpu) {
        this.vCpu = vCpu;
    }

    public int getRam() {
        return ram;
    }

    public void setRam(int ram) {
        this.ram = ram;
    }

    @Override
    public String toString() {
        return "Compute with " + getvCpu() + "vCPUS and " + getRam() + "MB of RAM";
    }
}
