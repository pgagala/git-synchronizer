package io.github.pgagala.util


class SpockMockitoVerifier {

    /**
     * Mockito verification doesn't fit spock's contract about checking boolean expression in "then" block.
     * It will return 0 value in case of the success so that should be transformed to true.
     */
    static <T> void toSpockVerification(T interaction) {
        interaction || true
    }

}
