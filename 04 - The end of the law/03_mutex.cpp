#include <iostream>
#include <thread>
#include <mutex>

using namespace std;

void firstChange(int &n, int newValue, mutex& mtx) {
    mtx.lock();
    this_thread::sleep_for(chrono::milliseconds(1000));
    n = newValue;
    mtx.unlock();
}

void secondChange(int& n, int newValue, mutex& mtx) {
    mtx.lock();
    n = newValue;
    mtx.unlock();
}

int main()
{
    int value = 0;
    mutex mtx;

    thread t1 = thread(firstChange, ref(value), 5, ref(mtx));
    thread t2 = thread(secondChange, ref(value), 10, ref(mtx));

    t1.join();
    t2.join();

    printf("Final result %d", value);

    return 0;
}