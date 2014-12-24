LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := stockfish
LOCAL_SRC_FILES := \
	application.cpp   evaluate.cpp  move.cpp      search.cpp \
	benchmark.cpp    history.cpp   movegen.cpp   tt.cpp \
	bitbase.cpp      main.cpp      movepick.cpp  uci.cpp \
	bitboard.cpp     pawns.cpp     ucioption.cpp \
	book.cpp         material.cpp  piece.cpp     value.cpp \
	direction.cpp    mersenne.cpp  position.cpp \
	endgame.cpp      misc.cpp      san.cpp

LOCAL_CFLAGS    := -mandroid \
	-DTARGET_OS=android -D__ANDROID__ \
	-isystem $(SYSROOT)/usr/include \
	-DNO_PREFETCH=1

include $(BUILD_STATIC_LIBRARY)
