/**
 * Created by Steve on 4/23/14.
 */
public class AlgorithmFactory
{
    public static void main(String args[]) {
        MutualExclusion alg = new Algorithm_3();

        // alg is the shared object including
        //   enter/leaveCriticalSection( )

        Worker first  = new Worker("Runner 0", 0, alg);
        Worker second = new Worker("Runner 1", 1, alg);
        first.start();    // first’s  thread id is 0
        second.start();   // second’s thread id is 1
    }
}