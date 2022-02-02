package ee.eid.installer;

public class CapFileInstaller {
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("To install application choose specific file.");
            System.out.println("Format: cap-file-installer {filename}");
        }
        System.out.println("Installation of the CAP file in progress!");

    }
}
