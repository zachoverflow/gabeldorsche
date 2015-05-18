CC=gcc
SRCS=*.c
CFLAGS=-Wall -Werror

ALL=*.c *.h

DEFINE=VERBOSE_LOGGING

beltd: $(ALL)
	$(CC) $(SRCS) -o beltd -lmraa -lm -lbluetooth -std=gnu11 -g -D $(DEFINE) $(CFLAGS)

clean:
	rm beltd
