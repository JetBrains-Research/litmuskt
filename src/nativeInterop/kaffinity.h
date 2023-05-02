#include <pthread.h>

int set_affinity(pthread_t thread, cpu_set_t* set);
int get_affinity(pthread_t thread, cpu_set_t* set);

void cpu_zero(cpu_set_t* set);
void cpu_set(int cpu, cpu_set_t* set);
int cpu_isset(int cpu, cpu_set_t* set);
int cpu_setsize();
