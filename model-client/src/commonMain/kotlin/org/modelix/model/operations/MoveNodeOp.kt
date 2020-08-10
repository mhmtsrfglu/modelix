/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License. 
 */

package org.modelix.model.operations

import org.modelix.model.api.IWriteTransaction

class MoveNodeOp(
    val childId: Long,
    val sourceParentId: Long,
    val sourceRole: String?,
    val sourceIndex: Int,
    val targetParentId: Long,
    val targetRole: String?,
    val targetIndex: Int
) : AbstractOperation() {
    fun withIndex(newSourceIndex: Int, newTargetIndex: Int): MoveNodeOp {
        return if (newSourceIndex == sourceIndex && newTargetIndex == targetIndex) {
            this
        } else {
            MoveNodeOp(childId, sourceParentId, sourceRole, newSourceIndex,
                targetParentId, targetRole, newTargetIndex)
        }
    }

    override fun apply(transaction: IWriteTransaction): IAppliedOperation {
        transaction.moveChild(targetParentId, targetRole, targetIndex, childId)
        return Applied()
    }

    override fun transform(previous: IOperation, indexAdjustments: IndexAdjustments): IOperation {
        return when (previous) {
            is AddNewChildOp -> this
            is DeleteNodeOp -> {
                if (previous.parentId == sourceParentId && previous.role == sourceRole && previous.index == sourceIndex) {
                    if (previous.childId != childId) {
                        throw RuntimeException("$sourceParentId.$sourceRole[$sourceIndex] expected to be ${childId.toString(16)}, but was ${previous.childId.toString(16)}")
                    }
                    indexAdjustments.nodeRemoved(targetParentId, targetRole, targetIndex)
                    NoOp()
                } else {
                    this
                }
            }
            is MoveNodeOp -> this
            is SetPropertyOp -> this
            is SetReferenceOp -> this
            is NoOp -> this
            else -> {
                throw RuntimeException("Unknown type: " + previous::class.simpleName)
            }
        }
    }

    override fun loadAdjustment(indexAdjustments: IndexAdjustments) {
        indexAdjustments.nodeRemoved(sourceParentId, sourceRole, sourceIndex)
        indexAdjustments.nodeAdded(targetParentId, targetRole, targetIndex)
    }

    override fun withAdjustedIndex(indexAdjustments: IndexAdjustments): IOperation {
        return withIndex(
            indexAdjustments.getAdjustedIndex(sourceParentId, sourceRole, sourceIndex),
            indexAdjustments.getAdjustedIndex(targetParentId, targetRole, targetIndex)
        )
    }

    override fun toString(): String {
        return "MoveNodeOp ${childId.toString(16)}, ${sourceParentId.toString(16)}.$sourceRole[$sourceIndex]->${targetParentId.toString(16)}.$targetRole[$targetIndex]"
    }

    inner class Applied : AbstractOperation.Applied(), IAppliedOperation {
        override val originalOp: IOperation
            get() = this@MoveNodeOp

        override fun invert(): IOperation {
            return MoveNodeOp(childId, targetParentId, targetRole, targetIndex, sourceParentId, sourceRole, sourceIndex)
        }
    }
}
