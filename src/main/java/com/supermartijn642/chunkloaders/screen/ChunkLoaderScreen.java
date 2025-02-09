package com.supermartijn642.chunkloaders.screen;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.supermartijn642.chunkloaders.ChunkLoaderTile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldVertexBufferUploader;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

/**
 * Created 7/11/2020 by SuperMartijn642
 */
public class ChunkLoaderScreen extends Screen {

    private static final ResourceLocation SCREEN_BACKGROUND = new ResourceLocation("chunkloaders", "textures/gui/background.png");

    protected World world;
    protected BlockPos pos;
    protected int left, top;

    private final int backgroundSize;

    private boolean doDrag = false;
    private boolean dragState = false;
    private List<ChunkButton> draggedButtons = new ArrayList<>();

    public ChunkLoaderScreen(String type, World world, BlockPos pos){
        super(new TranslationTextComponent("block.chunkloaders." + type));
        this.world = world;
        this.pos = pos;
        ChunkLoaderTile tile = this.getTileOrClose();
        int gridSize = tile == null ? 1 : tile.getGridSize();
        this.backgroundSize = gridSize * 15 + (gridSize - 1) + 16;
    }

    @Override
    protected void init(){
        ChunkLoaderTile tile = this.getTileOrClose();
        if(tile == null)
            return;

        this.left = (this.width - this.backgroundSize) / 2;
        this.top = (this.height - this.backgroundSize) / 2;

        int radius = (tile.getGridSize() - 1) / 2;
        for(int x = 0; x < tile.getGridSize(); x++){
            for(int y = 0; y < tile.getGridSize(); y++){
                this.addButton(new ChunkButton(this.left + 8 + x * 16, this.top + 8 + y * 16, -radius + x, -radius + y,
                    this::getTileOrClose, this.world, new ChunkPos((pos.getX() >> 4) - radius + x, (pos.getZ() >> 4) - radius + y)));
            }
        }
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks){
        this.renderBackground();
        this.drawBackgroundLayer(partialTicks, mouseX, mouseY);
        super.render(mouseX, mouseY, partialTicks);

        ChunkLoaderTile tile = this.getTileOrClose();
        if(tile == null)
            return;

        this.buttons.stream().filter(ChunkButton.class::isInstance).map(ChunkButton.class::cast).forEach(button -> {
            if(button.isHovered())
                this.renderToolTip(true, "chunkloaders.gui." + (tile.isLoaded(button.xOffset, button.zOffset) ? "loaded" : "unloaded"), mouseX, mouseY);
        });
    }

    private void drawBackgroundLayer(float partialTicks, int mouseX, int mouseY){
        ChunkLoaderTile tile = this.getTileOrClose();
        if(tile == null)
            return;

        drawScreenBackground(this.left, this.top, this.backgroundSize, this.backgroundSize);
    }

    protected void drawCenteredString(ITextComponent text, float x, float y){
        this.drawCenteredString(text.getColoredString(), x, y);
    }

    protected void drawCenteredString(String s, float x, float y){
        this.font.draw(s, this.left + x - this.font.width(s) / 2f, this.top + y, 4210752);
    }

    protected void drawString(ITextComponent text, float x, float y){
        this.drawString(text.getColoredString(), x, y);
    }

    protected void drawString(String s, float x, float y){
        this.font.draw(s, this.left + x, this.top + y, 4210752);
    }

    public void renderToolTip(boolean translate, String string, int x, int y){
        super.renderTooltip(translate ? new TranslationTextComponent(string).getColoredString() : string, x, y);
    }

    public ChunkLoaderTile getTileOrClose(){
        if(this.world != null && this.pos != null){
            TileEntity tile = this.world.getBlockEntity(this.pos);
            if(tile instanceof ChunkLoaderTile)
                return (ChunkLoaderTile)tile;
        }
        Minecraft.getInstance().player.closeContainer();
        return null;
    }

    @Override
    public boolean isPauseScreen(){
        return false;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button){
        if(button == 0){
            for(IGuiEventListener listener : this.children){
                if(listener instanceof ChunkButton){
                    ChunkButton chunkButton = (ChunkButton)listener;
                    if(chunkButton.isMouseOver(mouseX, mouseY)){
                        this.doDrag = true;
                        this.dragState = !chunkButton.isLoaded();
                        this.draggedButtons.clear();
                        this.draggedButtons.add(chunkButton);
                        chunkButton.onPress();
                        return true;
                    }
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY){
        if(this.doDrag && button == 0){
            for(IGuiEventListener listener : this.children){
                if(listener instanceof ChunkButton && !this.draggedButtons.contains(listener)){
                    ChunkButton chunkButton = (ChunkButton)listener;
                    if(chunkButton.isMouseOver(mouseX, mouseY) && chunkButton.isLoaded() != this.dragState){
                        chunkButton.onPress();
                        this.draggedButtons.add(chunkButton);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button){
        if(button == 0)
            this.doDrag = false;
        return false;
    }

    public void drawScreenBackground(float x, float y, float width, float height){
        Minecraft.getInstance().textureManager.bind(SCREEN_BACKGROUND);
        // corners
        drawTexture(x, y, 4, 4, 0, 0, 4 / 9f, 4 / 9f);
        drawTexture(x + width - 4, y, 4, 4, 5 / 9f, 0, 4 / 9f, 4 / 9f);
        drawTexture(x + width - 4, y + height - 4, 4, 4, 5 / 9f, 5 / 9f, 4 / 9f, 4 / 9f);
        drawTexture(x, y + height - 4, 4, 4, 0, 5 / 9f, 4 / 9f, 4 / 9f);
        // edges
        drawTexture(x + 4, y, width - 8, 4, 4 / 9f, 0, 1 / 9f, 4 / 9f);
        drawTexture(x + 4, y + height - 4, width - 8, 4, 4 / 9f, 5 / 9f, 1 / 9f, 4 / 9f);
        drawTexture(x, y + 4, 4, height - 8, 0, 4 / 9f, 4 / 9f, 1 / 9f);
        drawTexture(x + width - 4, y + 4, 4, height - 8, 5 / 9f, 4 / 9f, 4 / 9f, 1 / 9f);
        // center
        drawTexture(x + 4, y + 4, width - 8, height - 8, 4 / 9f, 4 / 9f, 1 / 9f, 1 / 9f);
    }

    public void drawTexture(float x, float y, float width, float height, float tx, float ty, float twidth, float theight){
        GlStateManager._color4f(1, 1, 1, 1);


        BufferBuilder bufferbuilder = Tessellator.getInstance().getBuilder();
        bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX);
        int z = this.getBlitOffset();
        bufferbuilder.vertex(x, y + height, z).uv(1, 0).endVertex();
        bufferbuilder.vertex(x + width, y + height, z).uv(1, 1).endVertex();
        bufferbuilder.vertex(x + width, y, z).uv(0, 1).endVertex();
        bufferbuilder.vertex(x, y, z).uv(0, 0).endVertex();
        bufferbuilder.end();
        RenderSystem.enableAlphaTest();
        WorldVertexBufferUploader.end(bufferbuilder);
    }

}
