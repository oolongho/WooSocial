package oolonghoo.woosocial.event;

import com.oolonghoo.woosocial.model.RelationData;
import com.oolonghoo.woosocial.module.relation.type.RelationType;
import org.bukkit.event.HandlerList;

import java.util.UUID;

/**
 * 关系确认事件
 * <p>
 * 当玩家确认接受关系申请时触发此事件，此事件不可取消
 * </p>
 *
 * @author oolongho
 * @since 1.0.0
 */
public class RelationAcceptEvent extends WooSocialEvent {
    
    private static final HandlerList handlers = new HandlerList();
    
    /**
     * 接受者 UUID（被申请的一方）
     */
    private final UUID accepterUuid;
    
    /**
     * 接受者名称
     */
    private final String accepterName;
    
    /**
     * 申请人 UUID
     */
    private final UUID proposerUuid;
    
    /**
     * 申请人名称
     */
    private final String proposerName;
    
    /**
     * 关系数据
     */
    private final RelationData relationData;
    
    /**
     * 关系类型
     */
    private final RelationType relationType;
    
    /**
     * 构造函数
     *
     * @param accepterUuid 接受者 UUID
     * @param accepterName 接受者名称
     * @param proposerUuid 申请人 UUID
     * @param proposerName 申请人名称
     * @param relationData 关系数据
     * @param relationType 关系类型
     */
    public RelationAcceptEvent(UUID accepterUuid, String accepterName,
                               UUID proposerUuid, String proposerName,
                               RelationData relationData, RelationType relationType) {
        super(true); // 关系确认涉及数据库操作，使用异步事件
        this.accepterUuid = accepterUuid;
        this.accepterName = accepterName;
        this.proposerUuid = proposerUuid;
        this.proposerName = proposerName;
        this.relationData = relationData;
        this.relationType = relationType;
    }
    
    /**
     * 获取接受者 UUID
     *
     * @return 接受者 UUID
     */
    public UUID getAccepterUuid() {
        return accepterUuid;
    }
    
    /**
     * 获取接受者名称
     *
     * @return 接受者名称
     */
    public String getAccepterName() {
        return accepterName;
    }
    
    /**
     * 获取申请人 UUID
     *
     * @return 申请人 UUID
     */
    public UUID getProposerUuid() {
        return proposerUuid;
    }
    
    /**
     * 获取申请人名称
     *
     * @return 申请人名称
     */
    public String getProposerName() {
        return proposerName;
    }
    
    /**
     * 获取关系数据
     *
     * @return 关系数据实例
     */
    public RelationData getRelationData() {
        return relationData;
    }
    
    /**
     * 获取关系类型
     *
     * @return 关系类型实例
     */
    public RelationType getRelationType() {
        return relationType;
    }
    
    /**
     * 获取关系类型 ID
     * <p>
     * 便捷方法，获取关系类型的 ID
     * </p>
     *
     * @return 关系类型 ID
     */
    public String getRelationTypeId() {
        return relationType != null ? relationType.getId() : null;
    }
    
    /**
     * 获取关系 ID
     * <p>
     * 便捷方法，从关系数据中获取 ID
     * </p>
     *
     * @return 关系 ID
     */
    public int getRelationId() {
        return relationData != null ? relationData.getId() : 0;
    }
    
    /**
     * 获取当前亲密度
     * <p>
     * 便捷方法，从关系数据中获取亲密度
     * </p>
     *
     * @return 亲密度值
     */
    public int getIntimacy() {
        return relationData != null ? relationData.getIntimacy() : 0;
    }
    
    @Override
    public HandlerList getHandlers() {
        return handlers;
    }
    
    public static HandlerList getHandlerList() {
        return handlers;
    }
}
