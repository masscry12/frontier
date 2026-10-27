# Frontier Pilots (mod Forge 1.20.1 inspirado em Titanfall 2)

Fase 1: **piloto + movimentação**. Estado: código escrito, ainda **não compilado nem testado em jogo**.

## Como rodar

Requisitos: **JDK 17** (Temurin/Adoptium) e internet na primeira vez (baixa Forge e Minecraft).

```
./gradlew runClient        # abre o Minecraft com o mod (Windows: gradlew.bat runClient)
./gradlew build            # gera build/libs/frontier-0.1.0.jar
```

Se o `gradlew` reclamar de versão do Gradle, use o 8.8 ou mais novo. Mande o log de erro completo e eu corrijo.

## Controles

| Ação | Como |
|---|---|
| Correr | Sprint normal (Ctrl / duplo W) |
| Pulo duplo | Pule, solte a tecla e aperte pulo de novo no ar |
| Wallrun | No ar, junto a uma parede, segurando W, em ângulo (não de frente) |
| Wall jump | Pule durante o wallrun (devolve o pulo duplo) |
| Slide | Tecla **C** (configurável) correndo; pode apertar um pouco antes de pousar |
| Slide-hop | Pule durante o slide |
| Controle aéreo | WASD no ar curva sem perder velocidade |

A action bar mostra velocidade (blocos/s), estado e pulos extras enquanto `debug = true`.

## Ajuste fino

`run/config/frontier-common.toml` (criado ao abrir o jogo). Tudo que define a "sensação" está lá, em
blocos/tick (1 b/tick = 20 b/s). Referências vanilla: andar 0,216, correr 0,28.

## Arquitetura

- `client/movement/PilotMovement` — toda a lógica, 1x por tick, no `MovementInputUpdateEvent`
  (depois do teclado e antes da física vanilla). Escreve no `deltaMovement`; a colisão continua vanilla.
- `client/movement/PilotMovementState` — estado (slide, wallrun, pulos usados, câmera).
- `client/ClientForgeEvents` — liga o evento de input, roll da câmera no wallrun e FOV por velocidade.
- `config/PilotConfig` — todos os parâmetros.
- `CommonEvents` — sem dano de queda.

Conservação de impulso: o arrasto de ar vanilla (0,91/tick) é desfeito e trocado por `momentumRetention`;
o atrito de chão perdido no tick do pulo é devolvido (bhop/slide-hop mantêm velocidade).

## Limitações conhecidas (fase 1)

- Em servidor dedicado, wallrun/flutuação pode acionar o anti-fly vanilla (use `allow-flight=true` por enquanto).
- Slide mantém a hitbox em pé (sem passar em vão de 1 bloco). Pose deitada exige mixin: próxima fase.
- Sons e partículas são placeholders vanilla; sem animações/modelos de piloto ainda.
- Outros jogadores ainda não veem wallrun/slide (precisa de pacote de rede).
- Valores de "feel" são um primeiro chute: devem ser ajustados jogando.
