<?php
/*
 *                   GridGain Community Edition Licensing
 *                   Copyright 2019 GridGain Systems, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License") modified with Commons Clause
 * Restriction; you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 *
 * Commons Clause Restriction
 *
 * The Software is provided to you by the Licensor under the License, as defined below, subject to
 * the following condition.
 *
 * Without limiting other conditions in the License, the grant of rights under the License will not
 * include, and the License does not grant to you, the right to Sell the Software.
 * For purposes of the foregoing, “Sell” means practicing any or all of the rights granted to you
 * under the License to provide to third parties, for a fee or other consideration (including without
 * limitation fees for hosting or consulting/ support services related to the Software), a product or
 * service whose value derives, entirely or substantially, from the functionality of the Software.
 * Any license notice or attribution required by the License must also include this Commons Clause
 * License Condition notice.
 *
 * For purposes of the clause above, the “Licensor” is Copyright 2019 GridGain Systems, Inc.,
 * the “License” is the Apache License, Version 2.0, and the Software is the GridGain Community
 * Edition software provided with this notice.
 */

namespace Apache\Ignite\Internal\Binary;

class BinaryField
{
    private $name;
    private $id;
    private $typeCode;
    
    public function __construct(string $name = null, int $typeCode = 0)
    {
        $this->name = $name;
        $this->id = BinaryField::calculateId($name);
        $this->typeCode = $typeCode;
    }
    
    public static function calculateId(?string $name): int
    {
        return BinaryUtils::hashCodeLowerCase($name);
    }

    public function getId(): int
    {
        return $this->id;
    }

    public function getName(): string
    {
        return $this->name;
    }

    public function getTypeCode(): int
    {
        return $this->typeCode;
    }

    public function isValid(): bool
    {
        return $this->name !== null;
    }

    public function write(MessageBuffer $buffer): void
    {
        // field name
        BinaryCommunicator::writeString($buffer, $this->name);
        // type code
        $buffer->writeInteger($this->typeCode);
        // field id
        $buffer->writeInteger($this->id);
    }

    public function read(MessageBuffer $buffer): void
    {
        // field name
        $this->name = BinaryCommunicator::readString($buffer);
        // type code
        $this->typeCode = $buffer->readInteger();
        // field id
        $this->id = $buffer->readInteger();
    }
}
