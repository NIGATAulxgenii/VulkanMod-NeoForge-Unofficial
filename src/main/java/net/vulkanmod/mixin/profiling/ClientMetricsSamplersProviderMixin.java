package net.vulkanmod.mixin.profiling;

import com.mojang.blaze3d.systems.TimerQuery;
import java.util.Optional;
import net.minecraft.client.profiling.ClientMetricsSamplersProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ClientMetricsSamplersProvider.class)
public class ClientMetricsSamplersProviderMixin {
   @Redirect(
      method = "registerStaticSamplers",
      at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/TimerQuery;getInstance()Ljava/util/Optional;")
   )
   private Optional<TimerQuery> preventTimerQuery() {
      return Optional.empty();
   }
}
