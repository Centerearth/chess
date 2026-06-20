import torch
import torch.nn as nn
from torch.utils.data import TensorDataset, DataLoader
import numpy as np
import os

script_dir = os.path.dirname(os.path.abspath(__file__))


class ChessEvaluator(nn.Module):
    def __init__(self):
        super().__init__()
        self.conv1 = nn.Conv2d(12, 64, 3, padding=1)
        self.conv2 = nn.Conv2d(64, 128, 3, padding=1)
        self.conv3 = nn.Conv2d(128, 128, 3, padding=1)
        self.fc1 = nn.Linear(128 * 8 * 8, 256)
        self.fc2 = nn.Linear(256, 1)

    def forward(self, x):
        x = torch.relu(self.conv1(x))
        x = torch.relu(self.conv2(x))
        x = torch.relu(self.conv3(x))
        x = x.flatten(1)
        x = torch.relu(self.fc1(x))
        return torch.tanh(self.fc2(x))


def train(positions_path, labels_path, epochs=10, batch_size=256, lr=1e-3):
    device = torch.device("mps" if torch.backends.mps.is_available() else "cpu")
    print(f"Training on {device}")

    X = torch.from_numpy(np.load(positions_path)).to(device)
    Y = torch.from_numpy(np.load(labels_path)).to(device)

    dataset = TensorDataset(X, Y)
    loader = DataLoader(dataset, batch_size=batch_size, shuffle=True)

    model = ChessEvaluator().to(device)
    optimizer = torch.optim.Adam(model.parameters(), lr=lr)
    criterion = nn.MSELoss()

    for epoch in range(epochs):
        total_loss = 0.0
        for X_batch, y_batch in loader:
            pred = model(X_batch).squeeze()
            loss = criterion(pred, y_batch)
            optimizer.zero_grad()
            loss.backward()
            optimizer.step()
            total_loss += loss.item()
        print(f"Epoch {epoch + 1}/{epochs}  loss={total_loss / len(loader):.4f}")

    return model


def export(model, output_path):
    model.cpu().eval()
    dummy = torch.zeros(1, 12, 8, 8)
    torch.onnx.export(
        model, dummy, output_path,
        input_names=["input"],
        output_names=["output"],
        dynamic_axes={"input": {0: "batch_size"}, "output": {0: "batch_size"}},
    )
    print(f"Exported to {output_path}")


if __name__ == "__main__":
    positions_path = os.path.join(script_dir, "output_positions.npy")
    labels_path = os.path.join(script_dir, "output_labels.npy")
    onnx_path = os.path.join(script_dir, "chess_eval.onnx")

    model = train(positions_path, labels_path, epochs=20)
    export(model, onnx_path)
