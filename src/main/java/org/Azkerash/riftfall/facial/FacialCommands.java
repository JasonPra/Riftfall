package org.Azkerash.riftfall.facial;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.function.Consumer;
import net.minecraft.commands.*;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.Azkerash.riftfall.Riftfall;

/** Temporary presentation overrides, not gameplay states. Compatible with any FacialParticipant. */
@EventBusSubscriber(modid=Riftfall.MODID)
public final class FacialCommands {
    @SubscribeEvent public static void register(RegisterCommandsEvent event) {
        var targets=Commands.argument("targets",EntityArgument.entities());
        var expressions=Commands.literal("expression");
        for (var expression:FaceExpression.values()) expressions.then(Commands.literal(expression.name().toLowerCase(java.util.Locale.ROOT))
            .executes(c->apply(c,p->p.facialController().animator().expression(expression))));
        expressions.then(Commands.literal("auto").executes(c->apply(c,p->p.facialController().animator().expression(null))));
        expressions.then(Commands.literal("cycle").executes(c->apply(c,p->p.facialController().animator().cycle())));
        var eyes=Commands.literal("eyes")
            .then(Commands.literal("auto").executes(c->apply(c,p->p.facialController().animator().gaze(null,null))))
            .then(Commands.literal("center").executes(c->apply(c,p->p.facialController().animator().gaze(0,0))))
            .then(Commands.literal("left").executes(c->apply(c,p->p.facialController().animator().gaze(-2,0))))
            .then(Commands.literal("right").executes(c->apply(c,p->p.facialController().animator().gaze(2,0))))
            .then(Commands.literal("up").executes(c->apply(c,p->p.facialController().animator().gaze(0,-1))))
            .then(Commands.literal("down").executes(c->apply(c,p->p.facialController().animator().gaze(0,1))));
        var blink=Commands.literal("blink")
            .then(Commands.literal("auto").executes(c->apply(c,p->p.facialController().animator().blink(-1))))
            .then(Commands.literal("open").executes(c->apply(c,p->p.facialController().animator().blink(0))))
            .then(Commands.literal("half").executes(c->apply(c,p->p.facialController().animator().blink(1))))
            .then(Commands.literal("closed").executes(c->apply(c,p->p.facialController().animator().blink(2))))
            .then(Commands.literal("once").executes(c->apply(c,p->p.facialController().blinkOnce())));
        var jaw=Commands.literal("jaw")
            .then(Commands.literal("auto").executes(c->apply(c,p->p.facialController().animator().jaw(-1))))
            .then(Commands.literal("talk").executes(c->apply(c,p->p.facialController().animator().talking(true))))
            .then(Commands.argument("opening",com.mojang.brigadier.arguments.IntegerArgumentType.integer(0,15)).executes(c->apply(c,p->p.facialController().animator().jaw(com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(c,"opening")))));
        targets.then(expressions).then(eyes).then(blink).then(jaw)
            .then(Commands.literal("reset").executes(c->apply(c,p->p.facialController().animator().reset())))
            .then(Commands.literal("inspect").executes(c->{
                int count=0;
                for (var entity:EntityArgument.getEntities(c,"targets")) if (entity instanceof FacialParticipant p) {
                    var frame=p.facialFrame();String text=entity.getDisplayName().getString()+" #"+entity.getId()+": "+frame.expression()
                        +" | jaw "+frame.jaw()+" | blink "+frame.blink()+" | eye X "+frame.gazeX()+" Y "+frame.gazeY();
                    var input=p.facialController().input();
                    String tracking=input.targetId()<0?" | idle gaze":String.format(java.util.Locale.ROOT," | tracking #%d | relative yaw %.1f pitch %.1f",input.targetId(),input.relativeYaw(),input.relativePitch());
                    c.getSource().sendSuccess(()->Component.literal(text+tracking),false);count++;
                }
                return count;
            }));
        event.getDispatcher().register(Commands.literal("riftfall").then(Commands.literal("face").requires(source->source.hasPermission(2)).then(targets)));
    }
    private static int apply(CommandContext<CommandSourceStack> context,Consumer<FacialParticipant> operation) throws CommandSyntaxException {
        int count=0;
        for (var entity:EntityArgument.getEntities(context,"targets")) if (entity instanceof FacialParticipant participant) { operation.accept(participant);count++; }
        if (count==0) context.getSource().sendFailure(Component.literal("No selected entities support facial animation."));
        else { int affected=count;context.getSource().sendSuccess(()->Component.literal("Facial preview updated for "+affected+" entities. Use face <targets> reset to restore automatic presentation."),false); }
        return count;
    }
    private FacialCommands() {}
}
