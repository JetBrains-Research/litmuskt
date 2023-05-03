#include <stdatomic.h>
#include <stdlib.h>

struct CSpinBarrier
{
    atomic_int waiting_count;
    atomic_int passed_barriers_count;
    int thread_count;
};

struct CSpinBarrier *create_barrier(int thread_count)
{
    struct CSpinBarrier *b = malloc(sizeof(struct CSpinBarrier));
    atomic_init(&b->waiting_count, 0);
    atomic_init(&b->passed_barriers_count, 0);
    b->thread_count = thread_count;
    return b;
}

void barrier_wait(struct CSpinBarrier *barrier)
{
    atomic_int *waiting_count_ptr = &barrier->waiting_count;
    atomic_int *passed_barriers_count_ptr = &barrier->passed_barriers_count;
    int thread_count = barrier->thread_count;

    int old_passed = atomic_load(passed_barriers_count_ptr);
    if (atomic_fetch_add(waiting_count_ptr, 1) == thread_count - 1)
    {
        atomic_store(waiting_count_ptr, 0);
        atomic_fetch_add(passed_barriers_count_ptr, 1);
    }
    else
    {
        while (atomic_load(passed_barriers_count_ptr) == old_passed)
        {
            atomic_load(passed_barriers_count_ptr); // spin
        }
    }
}
