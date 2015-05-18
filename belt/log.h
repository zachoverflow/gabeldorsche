#pragma once

#include <stdio.h>

#define LOG_ERROR(format, ...) (fprintf(stderr, format"\n", __VA_ARGS__))
#define LOG_INFO(format, ...) (fprintf(stderr, format"\n", __VA_ARGS__))

#ifdef VERBOSE_LOGGING
#define LOG_VERBOSE(format, ...) (fprintf(stderr, format"\n", __VA_ARGS__))
#else
#define LOG_VERBOSE(format, ...)
#endif
