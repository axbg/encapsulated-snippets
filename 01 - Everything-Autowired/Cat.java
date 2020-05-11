import java.util.Random;

@ManagedClass
public class Cat {
    private Scratcher scratcher;
    private Random fakeRandomGenerator = new Random();

    public int calculateDisposition(int humanAge) {
        int disposition = fakeRandomGenerator.nextInt() + (int) (0.3 * humanAge);

        if (disposition % 2 == 0) {
            scratcher.scratch();
        }

        return disposition;
    }
}