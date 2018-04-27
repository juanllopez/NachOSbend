package nachos.threads;
import nachos.ag.BoatGrader;

public class Boat
{
    static BoatGrader bg;
    private static Lock lock;

    private static Condition adultWaitOahu ;
    private static Condition childWaitMalokai ;
    private static Condition childWaitOahu ;
    private static Condition mainCondition ;
    private static Condition problemOfBoat;
    private static int numAdultsOahu;
    private static int numChildOahu;
    private static int numMalokaiAdult;
    private static int numMalokaiChild;
    private static boolean termino = false;


    private static boolean BoatOahu = true;
    private static boolean twochilds = false;

    public static int tc;
    public static int ta;


    public static void selfTest()
    {
        BoatGrader b = new BoatGrader();

        //System.out.println("\n **Testing Boats with only 2 children**");
        begin(3, 3, b);

//	System.out.println("\n **Testing Boats with 2 children, 1 adult**");
//  	begin(1, 2, b);

//  	System.out.println("\n **Testing Boats with 3 children, 3 adults**");
//  	begin(3, 3, b);
    }

    public static void begin( int adults, int children, BoatGrader b )
    {
        // Store the externally generated autograder in a class
        // variable to be accessible by children.
        bg = b;

        ta = adults;
        tc = children;

        // Instantiate global variables here

        int numAdultsOahu = 0;
        int numChildOahu = 0;
        int numMalokaiAdult = 0;
        int numMalokaiChild = 0;

        lock = new Lock();
        adultWaitOahu = new Condition(lock);
        childWaitOahu = new Condition(lock);
        childWaitMalokai = new Condition(lock);
        problemOfBoat = new Condition(lock);


        // Create threads here. See section 3.4 of the Nachos for Java
        // Walkthrough linked from the projects page.

        Runnable AdultRunner = new Runnable() {
            public void run() {
                AdultItinerary();
            }
        };

        Runnable ChildRunner = new Runnable() {
            public void run() {
                ChildItinerary();
            }
        };

        for (int i = 1; i <= adults; i++) {
            KThread Adult = new KThread(AdultRunner);

            Adult.setName("Adult Thread").fork();
        }

        for (int i = 1; i <= children; i++) {
            KThread Child = new KThread(ChildRunner);

            Child.setName("Child Thread").fork();
        }



        System.out.println("begin Children at oahi "+numChildOahu);
        /*lock.acquire();
        /*while(numMalokaiChild + numMalokaiAdult != ta +tc)
        { problemOfBoat.sleep();}
        lock.release();*/
    }
    static boolean done(int tc, int ta){
        if(tc==numMalokaiChild&&ta==numMalokaiAdult&&BoatOahu == false){
            termino = true;
            return true;
        }
        else
            return false;
    }

    static void AdultItinerary()
    {
        lock.acquire();
        numAdultsOahu += 1;
	/* This is where you should put your solutions. Make calls
	   to the BoatGrader to show that it is synchronized. For
	   example:
	       bg.AdultRowToMolokai();
	   indicates that an adult has rowed the boat across to Molokai
	*/

        while ((!BoatOahu || numChildOahu != 1)
                //&&!termino
        ){

            adultWaitOahu.sleep();
            childWaitOahu.wake();

        }

        numAdultsOahu -= 1;
        bg.AdultRowToMolokai();

        BoatOahu = false;
        numMalokaiAdult += 1;

        childWaitMalokai.wake();
        lock.release();

    }

    static void ChildItinerary()
    {
        lock.acquire();
        numChildOahu ++;

        while ((ta + tc != numMalokaiChild + numMalokaiAdult)
                //&&!termino
        ){

            if (BoatOahu && numChildOahu > 1){

                if (twochilds){

                    numChildOahu -=2;

                    bg.ChildRideToMolokai();
                    BoatOahu = false;
                    numMalokaiChild += 2;
                    twochilds = false;

                    childWaitMalokai.wake();
                    adultWaitOahu.wake();
                    childWaitMalokai.sleep();

                } else {

                    bg.ChildRowToMolokai();

                    twochilds = true;
                    childWaitOahu.wake();
                    childWaitMalokai.sleep();
                }

            }
            else if (!BoatOahu) {

                childWaitMalokai.wake();

                numMalokaiChild --;
                bg.ChildRowToOahu();
                BoatOahu = true;

                numChildOahu ++;
                adultWaitOahu.wake();
                childWaitOahu.sleep();
            }
            else {

                adultWaitOahu.wake();
                childWaitOahu.sleep();
            }

        }
        childWaitMalokai.wakeAll();
        childWaitOahu.wakeAll();
        //System.out.println("\n hola");
        lock.release();
        //System.out.println("\n hola");
        return;
    }

    static void SampleItinerary()
    {
        // Please note that this isn't a valid solution (you can't fit
        // all of them on the boat). Please also note that you may not
        // have a single thread calculate a solution and then just play
        // it back at the autograder -- you will be caught.
        System.out.println("\n **Everyone piles on the boat and goes to Molokai**");
        bg.AdultRowToMolokai();
        bg.ChildRideToMolokai();
        bg.AdultRideToMolokai();
        bg.ChildRideToMolokai();

    }



}