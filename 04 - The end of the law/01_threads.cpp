#include <iostream>
#include <thread>

using namespace std;

void doLoop(int n, const char* initiator) {
    for (int i = 0; i < n; i++) {
        printf("\n%s iteration %d \n", initiator, i);
        this_thread::sleep_for(chrono::milliseconds(200));
    }
}

int main()
{
    int iterations = 10;

    thread t = thread(doLoop, iterations, "thread");

    doLoop(iterations, "main_thread");

    t.join();

    return 0;
}