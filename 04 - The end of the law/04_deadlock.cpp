#include <iostream>
#include <thread>
#include <mutex>

using namespace std;

void compute(mutex& mtx1, mutex& mtx2) {
    mtx1.lock();
    this_thread::sleep_for(chrono::milliseconds(200));

   //  both threads are waiting to lock the second mutex
   // which is already locked by the other thread
    mtx2.lock();

   // this line will never be printed
    printf("both resources locked - finished");

    mtx2.unlock();
    mtx1.unlock();
}


int main()
{
    int iterations = 10;

    mutex mtx1, mtx2;

    // we pass the mutexes in alternative order
    thread t1 = thread(compute, ref(mtx1), ref(mtx2));
    thread t2 = thread(compute, ref(mtx2), ref(mtx1));

    t1.join();
    t2.join();

    return 0;
}