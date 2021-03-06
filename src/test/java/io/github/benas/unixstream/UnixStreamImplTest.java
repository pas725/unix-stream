package io.github.benas.unixstream;

import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;

import static io.github.benas.unixstream.UnixStream.file;
import static io.github.benas.unixstream.components.Grep.grep;
import static io.github.benas.unixstream.components.NumberLines.nl;
import static io.github.benas.unixstream.components.Sort.sort;
import static io.github.benas.unixstream.components.Uniq.uniq;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

public class UnixStreamImplTest {

    private Stream<String> stream;

    private UnixStream<String> unixStream;

    @Before
    public void setUp() {
        stream = Stream.of("id,name", "1,foo", "2,bar");
    }

    @Test
    public void compact() throws Exception {
        stream = Stream.of(" f o o       ");
        unixStream = new UnixStreamImpl<>(stream);

        assertThat(unixStream.compact()).containsExactly("foo");
    }

    @Test
    public void concat() throws Exception {
        stream = Stream.of("foo");
        unixStream = new UnixStreamImpl<>(stream);

        assertThat(unixStream.concat(Stream.of("bar"))).containsExactly("foo", "bar");
    }

    @Test
    public void cut() throws Exception {
        stream = Stream.of("1;foo");
        unixStream = new UnixStreamImpl<>(stream);

        assertThat(unixStream.cut(";", 2)).containsExactly("foo");
    }

    @Test
    public void dos2unix() throws Exception {
        stream = Stream.of("a\r\n", "b\r\nc\r\n");
        unixStream = new UnixStreamImpl<>(stream);

        assertThat(unixStream.dos2unix()).containsExactly("a\n", "b\nc\n");
    }

    @Test
    public void getShouldReturnTheOriginalStream() throws Exception {
        assertThat(UnixStream.unixify(stream).get()).isEqualTo(stream);
    }

    @Test
    public void integrationTest1() throws IOException {
        List<String> strings = UnixStream.unixify(stream)
                .grep("bar")
                .cut(",", 2)
                .sort()
                .uniq()
                .nl()
                .collect(toList());

        assertThat(strings).isNotEmpty().isEqualTo(singletonList("1 bar"));
    }

    @Test
    public void integrationTest2() throws IOException {
        // cat input.txt | grep a | sort | uniq | nl > output.txt
        UnixStream.cat("src/test/resources/input.txt")
                .pipe(grep("a"))
                .pipe(sort())
                .pipe(uniq())
                .pipe(nl())
                .to(file("target/output.txt"));

        assertThat(new File("target/output.txt")).exists().hasContent("1 2,bar");
    }

    @Test
    public void integrationTest3() throws IOException {
        List<String> items = stream
                .filter(Predicates.grep("a"))
                .map(Functions.cut(",", 2))
                .collect(toList());

        assertThat(items).isNotNull().isNotEmpty().hasSize(2).containsExactly("name", "bar");
    }

}
