/*
 * PermissionsEx
 * Copyright (C) zml and PermissionsEx contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ca.stellardrift.permissionsex.util.command;

import ca.stellardrift.permissionsex.util.command.args.*;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static net.kyori.text.TextComponent.of;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for all argument types contained in GenericArguments
 */
public class GenericArgumentsTest {
    static final CommandExecutor NULL_EXECUTOR = (src, args) -> {
    };

    private static CommandContext parseForInput(String input, CommandElement element) throws ArgumentParseException {
        CommandSpec spec = CommandSpec.builder()
                .setAliases("test")
                .setExecutor(NULL_EXECUTOR)
                .build();
        CommandArgs args = QuotedStringParser.parseFrom(input, false);
        CommandContext context = new CommandContext(spec, args.getRaw());
        element.parse(args, context);
        return context;
    }

    @Test
    public void testNone() throws ArgumentParseException {
        CommandArgs args = QuotedStringParser.parseFrom("a", false);
        CommandContext context = new CommandContext(CommandSpec.builder().setAliases("test").setExecutor(NULL_EXECUTOR).build(), args.getRaw());
        GenericArguments.none().parse(args, context);
        assertEquals("a", args.next());
    }

    @Test
    @Disabled
    public void testFlags() {
        throw new UnsupportedOperationException();
    }

    @Test
    public void testSequence() throws ArgumentParseException {
        CommandElement el = GenericArguments.seq(GenericArguments.string(of("one")), GenericArguments.string(of("two")), GenericArguments.string(of("three")));
        CommandContext context = parseForInput("a b c", el);
        assertEquals("a", context.getOne("one"));
        assertEquals("b", context.getOne("two"));
        assertEquals("c", context.getOne("three"));

        Assertions.assertThrows(ArgumentParseException.class, () -> {
            parseForInput("a b", el);
        });
    }

    @Test
    public void testChoices() throws ArgumentParseException {
        CommandElement el = GenericArguments.choices(of("val"), ImmutableMap.of("a", "one", "b", "two"));
        CommandContext context = parseForInput("a", el);
        assertEquals("one", context.getOne("val"));

        Assertions.assertThrows(ArgumentParseException.class, () -> {
            parseForInput("c", el);
        });
    }

    @Test
    public void testFirstParsing() throws ArgumentParseException {
        CommandElement el = GenericArguments.firstParsing(GenericArguments.integer(of("val")), GenericArguments.string(of("val")));
        CommandContext context = parseForInput("word", el);
        assertEquals("word", context.getOne("val"));

        context = parseForInput("42", el);
        assertEquals((Integer) 42, context.getOne("val"));
    }

    @Test
    public void testOptional() throws ArgumentParseException {
        CommandElement el = GenericArguments.optional(GenericArguments.string(of("val")));
        CommandContext context = parseForInput("", el);
        assertNull(context.getOne("val"));

        el = GenericArguments.optional(GenericArguments.string(of("val")), "def");
        context = parseForInput("", el);
        assertEquals("def", context.getOne("val"));

        el = GenericArguments.seq(GenericArguments.optionalWeak(GenericArguments.integer(of("val"))), GenericArguments.string(of("str")));
        context = parseForInput("hello", el);
        assertEquals("hello", context.getOne("str"));

        el = GenericArguments.seq(GenericArguments.optional(GenericArguments.integer(of("val")), GenericArguments.string(of("str"))));
        final CommandElement finalEl = el;
        Assertions.assertThrows(ArgumentParseException.class, () -> parseForInput("hello", finalEl));
    }

    @Test
    public void testRepeated() throws ArgumentParseException {
        CommandContext context = parseForInput("1 1 2 3 5", GenericArguments.repeated(GenericArguments.integer(of("key")), 5));
        assertEquals(ImmutableList.<Object>of(1, 1, 2, 3, 5), context.getAll("key"));
    }

    @Test
    public void testAllOf() throws ArgumentParseException {
        CommandContext context = parseForInput("2 4 8 16 32 64 128", GenericArguments.allOf(GenericArguments.integer(of("key"))));
        assertEquals(ImmutableList.<Object>of(2, 4, 8, 16, 32, 64, 128), context.getAll("key"));
    }

    @Test
    public void testString() throws ArgumentParseException {
        CommandContext context = parseForInput("\"here it is\"", GenericArguments.string(of("a value")));
        assertEquals("here it is", context.getOne("a value"));
    }

    @Test
    public void testInteger() throws ArgumentParseException {
        CommandContext context = parseForInput("52", GenericArguments.integer(of("a value")));
        assertEquals((Integer) 52, context.getOne("a value"));

        assertThrows(ArgumentParseException.class, () -> parseForInput("notanumber", GenericArguments.integer(of("a value"))));
    }

    @Test
    public void testUUID() throws ArgumentParseException {
        final UUID subject = UUID.randomUUID();
        CommandContext ctx = parseForInput(subject.toString(), GenericArguments.uuid(of("a value")));
        assertEquals(subject, ctx.getOne("a value"));


        assertThrows(ArgumentParseException.class, () -> parseForInput("48459", GenericArguments.uuid(of("a value"))));
        assertThrows(ArgumentParseException.class, () -> parseForInput("words", GenericArguments.uuid(of("a value"))));
    }

    @Test
    public void testBool() throws ArgumentParseException {
        CommandElement boolEl = GenericArguments.bool(of("val"));
        assertEquals(true, parseForInput("true", boolEl).getOne("val"));
        assertEquals(true, parseForInput("t", boolEl).getOne("val"));
        assertEquals(false, parseForInput("f", boolEl).getOne("val"));


        assertThrows(ArgumentParseException.class, () -> parseForInput("notabool", boolEl));
    }

    private enum TestEnum {
        ONE, TWO, RED
    }

    @Test
    public void testEnumValue() throws ArgumentParseException {
        CommandElement enumEl = GenericArguments.enumValue(of("val"), TestEnum.class);
        assertEquals(TestEnum.ONE, parseForInput("one", enumEl).getOne("val"));
        assertEquals(TestEnum.TWO, parseForInput("TwO", enumEl).getOne("val"));
        assertEquals(TestEnum.RED, parseForInput("RED", enumEl).getOne("val"));

        assertThrows(ArgumentParseException.class, () -> parseForInput("notanel", enumEl));
    }

    @Test
    public void testRemainingJoinedStrings() throws ArgumentParseException {
        CommandElement remainingJoined = GenericArguments.remainingJoinedStrings(of("val"));
        assertEquals("one", parseForInput("one", remainingJoined).getOne("val"));
        assertEquals("one big string", parseForInput("one big string", remainingJoined).getOne("val"));
    }
}
